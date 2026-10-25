import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';

export function insidePath(root, relative) {
  if (!relative || relative.includes('\\') || path.posix.isAbsolute(relative) || /^[A-Za-z]:/.test(relative)) throw new Error('Invalid snapshot path.');
  const segments = relative.split('/');
  if (segments.some(s => !s || s === '.' || s === '..')) throw new Error('Snapshot path escaped its directory.');
  const result = path.resolve(root, ...segments);
  if (!result.startsWith(path.resolve(root) + path.sep)) throw new Error('Snapshot path escaped its directory.');
  let current = path.resolve(root);
  for (const segment of segments) {
    current = path.join(current, segment);
    if (fs.existsSync(current) && fs.lstatSync(current).isSymbolicLink()) throw new Error('Snapshot paths must not contain symlinks.');
  }
  return result;
}

export function sourceVersions(root) {
  return fs.readdirSync(path.join(root, 'up-self-admin/src/main/resources/db/draft'))
    .map(name => /^V([0-9]+)__.*\.sql$/.exec(name)?.[1]).filter(Boolean).sort((a, b) => Number(a) - Number(b));
}

async function hashFile(file) {
  const hash = crypto.createHash('sha256');
  for await (const chunk of fs.createReadStream(file)) hash.update(chunk);
  return hash.digest('hex');
}

export async function validateSnapshot(directory, versions) {
  const root = fs.realpathSync(directory);
  const manifest = JSON.parse(fs.readFileSync(insidePath(root, 'manifest.json'), 'utf8').replace(/^\uFEFF/, ''));
  if (manifest.database !== 'up_self_dev' || manifest.mysql !== '5.7.44') throw new Error('Snapshot must be for independent up_self_dev on MySQL 5.7.44.');
  if (!Array.isArray(manifest.flywayVersions) || manifest.flywayVersions.map(String).join(',') !== versions.join(',')) {
    throw new Error('Snapshot is older than or differs from the source migrations. Export the latest database and uploads on Windows first; the archived V5 snapshot is not the latest data.');
  }
  const files = manifest.files;
  if (!files || !files['up_self_dev.sql'] || !files['local-credentials.json']) throw new Error('Incomplete snapshot manifest.');
  for (const [table, count] of Object.entries(manifest.rowCounts || {})) {
    if (!/^[a-z][a-z0-9_]*$/.test(table) || !Number.isInteger(count) || count < 0) throw new Error('Invalid snapshot table count.');
  }
  let uploadCount = 0;
  for (const [relative, expected] of Object.entries(files)) {
    if (!['up_self_dev.sql', 'local-credentials.json'].includes(relative) && !/^uploads\/(?:assignments\/)?[a-f0-9]{32}\.[a-zA-Z0-9]+$/.test(relative)) throw new Error('Unexpected snapshot file.');
    const file = insidePath(root, relative);
    if (!fs.existsSync(file) || !fs.statSync(file).isFile()) throw new Error(`Snapshot file missing: ${relative}`);
    if (!/^[a-f0-9]{64}$/i.test(expected.sha256) || fs.statSync(file).size !== expected.bytes || await hashFile(file) !== expected.sha256.toLowerCase()) throw new Error(`Snapshot checksum mismatch: ${relative}`);
    if (relative.startsWith('uploads/')) uploadCount++;
  }
  if (uploadCount !== manifest.uploadFileCount) throw new Error('Snapshot upload count mismatch.');
  return { root, manifest };
}

export function assertEmptyUploadTarget(project) {
  // Refuse symlinks and any existing entries, including hidden files/directories.
  const target = insidePath(project, '.local/uploads');
  if (fs.existsSync(target) && (!fs.statSync(target).isDirectory() || fs.readdirSync(target).length)) {
    throw new Error('Refusing to merge uploads into a non-empty target. Restore into a fresh clone; existing files were preserved.');
  }
  return target;
}

function invokeMySql(executable, args, { sql, inputFile, env } = {}) {
  return new Promise((resolve, reject) => {
    const inputFd = inputFile ? fs.openSync(inputFile, 'r') : null;
    let child;
    try { child = spawn(executable, args, { env: env || process.env, stdio: [inputFd ?? 'pipe', 'pipe', 'pipe'] }); }
    finally { if (inputFd !== null) fs.closeSync(inputFd); }
    let output = '';
    // Do not echo SQL or credentials, even if mysql includes them in an error.
    child.stderr.resume();
    child.stdout.on('data', chunk => { if (output.length < 1024 * 1024) output += chunk.toString('utf8'); });
    child.once('error', () => reject(new Error('Could not start the configured MySQL client.')));
    child.once('close', code => code === 0 ? resolve(output.trim()) : reject(new Error(`MySQL operation failed (exit ${code}). Check server state, the private root client file and account permissions. SQL and credentials were not logged. Stop after a failed restore; do not retry into the partially restored database.`)));
    if (!inputFile) {
      child.stdin.on('error', () => {});
      child.stdin.end(sql || '');
    }
  });
}

function credentialsFrom(directory) {
  const file = insidePath(fs.realpathSync(directory), 'local-credentials.json');
  const credentials = JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  for (const role of ['app', 'migration']) {
    if (!/^[a-zA-Z][a-zA-Z0-9_]{0,31}$/.test(credentials[`mysql_${role}_user`]) || typeof credentials[`mysql_${role}_password`] !== 'string') throw new Error('Invalid scoped credentials in snapshot.');
  }
  return credentials;
}

export async function databaseAction(action, options, tools, project) {
  if (!['db-check', 'db-init', 'db-restore'].includes(action)) throw new Error('Use db-check, db-init or db-restore.');
  for (const name of Object.keys(options)) if (!['--root-client-file', '--snapshot'].includes(name)) throw new Error(`Unknown database option ${name}.`);
  if (!options['--root-client-file']) throw new Error('Pass --root-client-file with your private Mac MySQL root client file. No root password is changed automatically.');
  const rootFile = fs.realpathSync(options['--root-client-file']);
  if (!fs.statSync(rootFile).isFile() || (fs.statSync(rootFile).mode & 0o077)) throw new Error('The root client file must be a regular private file: chmod 600 PATH.');
  const mysql = path.join(tools.mysqlHome, 'bin/mysql');
  const args = [`--defaults-file=${rootFile}`, '--connect-timeout=5', '--default-character-set=utf8mb4', '--batch', '--skip-column-names'];
  const query = sql => invokeMySql(mysql, args, { sql });
  const server = await query('SELECT VERSION();');
  if (!/^5\.7\.44(?:-|$)/.test(server)) throw new Error('The running MySQL server must be 5.7.44. No server version or password was changed.');
  const count = Number(await query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='up_self_dev';"));
  if (!Number.isInteger(count)) throw new Error('Unable to determine target table count.');
  if (action === 'db-check') {
    console.log(`MySQL 5.7.44 connection OK. up_self_dev contains ${count} tables. No database changes made.`);
    if (count) {
      const hasHistory = await query("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='up_self_dev' AND table_name='flyway_schema_history';");
      if (hasHistory === '1') console.log('Applied migrations: ' + await query('SELECT version FROM up_self_dev.flyway_schema_history WHERE success=1 ORDER BY installed_rank;'));
    }
    return;
  }
  if (count !== 0) throw new Error('Refusing to initialize or restore into non-empty up_self_dev. Existing data was preserved.');
  const snapshot = path.resolve(options['--snapshot'] || path.join(project, 'migration/local-snapshot'));
  const versions = sourceVersions(project);
  let validated;
  let uploadTarget;
  if (action === 'db-restore') {
    validated = await validateSnapshot(snapshot, versions);
    uploadTarget = assertEmptyUploadTarget(project);
  }
  const credentials = credentialsFrom(snapshot);
  // Preserve reference setup: separate application DML user and migration user.
  let setup = "SET SESSION sql_mode='NO_BACKSLASH_ESCAPES'; CREATE DATABASE IF NOT EXISTS up_self_dev CHARACTER SET utf8 COLLATE utf8_general_ci;";
  for (const role of ['app', 'migration']) {
    const user = credentials[`mysql_${role}_user`];
    const password = credentials[`mysql_${role}_password`].replaceAll("'", "''");
    const grants = role === 'app' ? 'SELECT,INSERT,UPDATE,DELETE' : 'ALL PRIVILEGES';
    for (const host of ['localhost', '127.0.0.1']) setup += `CREATE USER IF NOT EXISTS '${user}'@'${host}' IDENTIFIED BY '${password}';GRANT ${grants} ON up_self_dev.* TO '${user}'@'${host}';`;
  }
  await query(setup);
  for (const role of ['app', 'migration']) {
    await invokeMySql(mysql, ['--no-defaults', '--protocol=TCP', '--host=127.0.0.1', '--port=3306', `--user=${credentials[`mysql_${role}_user`]}`, '--connect-timeout=5', '--batch', '--skip-column-names', '--database=up_self_dev'], {
      env: { ...process.env, MYSQL_PWD: credentials[`mysql_${role}_password`] }, sql: 'SELECT 1;',
    });
  }
  if (action === 'db-init') {
    console.log(`Empty up_self_dev and scoped users are ready. Start the backend to apply V${versions.join(', V')}. No old business data was imported.`);
    return;
  }
  await invokeMySql(mysql, [...args, '--database=up_self_dev'], { inputFile: path.join(validated.root, 'up_self_dev.sql') });
  const applied = (await query('SELECT version FROM up_self_dev.flyway_schema_history WHERE success=1 ORDER BY installed_rank;')).split(/\s+/);
  if (applied.join(',') !== versions.join(',')) throw new Error('Restored Flyway history does not match the current source. Stop and investigate before launching.');
  for (const [table, expected] of Object.entries(validated.manifest.rowCounts || {})) {
    if (!/^[a-z][a-z0-9_]*$/.test(table) || !Number.isInteger(expected) || expected < 0) throw new Error('Invalid snapshot table count.');
    const actual = Number(await query(`SELECT COUNT(*) FROM up_self_dev.\`${table}\`;`));
    if (actual !== expected) throw new Error(`Restored row count differs for ${table}; stop and investigate.`);
  }
  fs.mkdirSync(uploadTarget, { recursive: true });
  for (const relative of Object.keys(validated.manifest.files).filter(name => name.startsWith('uploads/'))) {
    const destination = insidePath(uploadTarget, relative.slice('uploads/'.length));
    fs.mkdirSync(path.dirname(destination), { recursive: true });
    fs.copyFileSync(insidePath(validated.root, relative), destination, fs.constants.COPYFILE_EXCL);
    if (await hashFile(destination) !== validated.manifest.files[relative].sha256.toLowerCase()) throw new Error('Copied upload checksum failed. Stop and investigate.');
  }
  console.log(`Restored up_self_dev, V${versions.join(', V')} and ${validated.manifest.uploadFileCount} matching uploads. Existing root and account passwords were not reset. Re-login; Redis sessions were not migrated.`);
}
