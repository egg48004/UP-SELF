// Runs on Windows too. These are preparation checks, not a macOS runtime test.
import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import crypto from 'node:crypto';
import { parseOptions, assertVersion, validateMacVersion, generateWorkspace, projectRoot } from './tools.mjs';
import { insidePath, sourceVersions, validateSnapshot, assertEmptyUploadTarget } from './database.mjs';

const scratch = fs.mkdtempSync(path.join(os.tmpdir(), 'up-self-mac-'));
test.after(() => {
  assert.equal(path.dirname(path.resolve(scratch)), path.resolve(os.tmpdir()));
  assert.ok(path.basename(scratch).startsWith('up-self-mac-'));
  fs.rmSync(scratch, { recursive: true, force: true });
});

test('Node 24 macOS boundary rejects 13.2.1 and accepts 13.5+', () => {
  assert.throws(() => validateMacVersion('13.2.1'), /13.5/);
  validateMacVersion('13.5'); validateMacVersion('14.0'); validateMacVersion('15.1.2');
});
test('options preserve spaces and reject missing or duplicate values', () => {
  assert.deepEqual(parseOptions(['--java-home', '/Volumes/Dev Tools/JDK']), { '--java-home': '/Volumes/Dev Tools/JDK' });
  assert.throws(() => parseOptions(['--java-home']));
  assert.throws(() => parseOptions(['--java-home', '/a', '--java-home', '/b']));
});
test('Mac Redis baseline maps to upstream 5.0.14, not the Windows suffix or latest', () => {
  assertVersion('redis', 'Redis server v=5.0.14 sha=00000000:0 malloc=libc bits=64');
  assert.throws(() => assertVersion('redis', 'Redis server v=5.0.14.1 sha=0'));
  assert.throws(() => assertVersion('redis', 'Redis server v=7.4.0 sha=0'));
});
test('wrong Java, compiler, MySQL and npm versions stop configuration', () => {
  assertVersion('java', 'java version "1.8.0_231"\nJava(TM) SE Runtime Environment');
  assertVersion('javac', 'javac 1.8.0_231');
  assertVersion('mysql', 'mysql Ver 14.14 Distrib 5.7.44, for macos10.13 (x86_64)');
  for (const [name, text] of [['java', 'java version "17.0.1"'], ['javac', 'javac 1.8.0_232'], ['mysql', 'Distrib 8.0.29'], ['npm', '11.20.0']]) assert.throws(() => assertVersion(name, text));
});
test('generated workspace points to project from .local/macos and pins executable paths', () => {
  const template = JSON.parse(fs.readFileSync(path.join(projectRoot, 'up-self-macos.code-workspace'), 'utf8'));
  const original = structuredClone(template);
  const tools = Object.fromEntries(['java', 'maven', 'node', 'mysql', 'redis'].map(name => [`${name}Home`, `/Applications/Dev Tools/${name}`]));
  const result = generateWorkspace(template, tools, '/Users/demo/Projects/UP SELF');
  assert.equal(result.folders[0].path, '../..');
  assert.equal(result.settings['java.configuration.runtimes'][0].path, tools.javaHome);
  assert.equal(result.launch.configurations[0].javaExec, path.join(tools.javaHome, 'bin/java'));
  assert.equal(result.launch.configurations[1].runtimeExecutable, path.join(tools.nodeHome, 'bin/node'));
  assert.ok(result.settings['terminal.integrated.env.osx'].PATH.includes('Dev Tools'));
  const labels = new Set(result.tasks.tasks.map(task => task.label));
  assert.ok(labels.has(result.launch.configurations[0].preLaunchTask));
  const names = new Set(result.launch.configurations.map(config => config.name));
  for (const name of result.launch.compounds[0].configurations) assert.ok(names.has(name));
  assert.deepEqual(template, original);
});
test('snapshot paths reject traversal, absolute Windows/Unix paths and empty segments', () => {
  for (const relative of ['../outside', '/tmp/outside', 'C:/outside', 'uploads/../../outside', 'uploads\\outside', 'uploads//outside', './outside']) assert.throws(() => insidePath(scratch, relative));
});
test('restore refuses pre-existing hidden files and directories', () => {
  const project = path.join(scratch, 'occupied');
  fs.mkdirSync(path.join(project, '.local/uploads'), { recursive: true });
  fs.writeFileSync(path.join(project, '.local/uploads/.DS_Store'), 'preserve');
  assert.throws(() => assertEmptyUploadTarget(project), /non-empty/);
  assert.equal(fs.readFileSync(path.join(project, '.local/uploads/.DS_Store'), 'utf8'), 'preserve');
  assert.ok(assertEmptyUploadTarget(path.join(scratch, 'fresh')).endsWith(path.join('.local', 'uploads')));
});

function snapshotFixture(name) {
  const directory = path.join(scratch, name);
  fs.mkdirSync(path.join(directory, 'uploads'), { recursive: true });
  const versions = sourceVersions(projectRoot);
  const upload = `uploads/${'a'.repeat(32)}.png`;
  const files = {};
  for (const [relative, content] of [['up_self_dev.sql', '-- fixture schema only'], ['local-credentials.json', '{}'], [upload, 'fixture media']]) {
    fs.writeFileSync(path.join(directory, relative), content);
    files[relative] = { bytes: Buffer.byteLength(content), sha256: crypto.createHash('sha256').update(content).digest('hex') };
  }
  const manifest = { database: 'up_self_dev', mysql: '5.7.44', flywayVersions: versions, uploadFileCount: 1, files };
  const save = () => fs.writeFileSync(path.join(directory, 'manifest.json'), JSON.stringify(manifest));
  save();
  return { directory, manifest, save, versions, upload };
}

test('complete current snapshot passes integrity preflight without database access', async () => {
  const fixture = snapshotFixture('valid');
  const result = await validateSnapshot(fixture.directory, fixture.versions);
  assert.equal(result.manifest.uploadFileCount, 1);
  assert.equal(result.manifest.flywayVersions.at(-1), '9');
});
test('old V5 snapshot is rejected before restore', async () => {
  const fixture = snapshotFixture('old'); fixture.manifest.flywayVersions = ['1', '2', '3', '4', '5']; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /older/);
});
test('modified media fails checksum preflight', async () => {
  const fixture = snapshotFixture('changed'); fs.writeFileSync(path.join(fixture.directory, fixture.upload), 'modified media');
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /checksum/);
});
test('missing attachment fails preflight', async () => {
  const fixture = snapshotFixture('missing'); fs.unlinkSync(path.join(fixture.directory, fixture.upload));
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /missing/);
});
test('wrong database or missing credential manifest cannot be imported', async () => {
  const fixture = snapshotFixture('wrong-db'); fixture.manifest.database = 'reference_database'; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /independent/);
  fixture.manifest.database = 'up_self_dev'; delete fixture.manifest.files['local-credentials.json']; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /Incomplete/);
});
test('incorrect upload counts cannot pass preflight', async () => {
  const fixture = snapshotFixture('wrong-count'); fixture.manifest.uploadFileCount = 2; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /count/);
});
test('manifest cannot name files outside the snapshot', async () => {
  const fixture = snapshotFixture('escape'); fixture.manifest.files['../outside'] = fixture.manifest.files['up_self_dev.sql']; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /Unexpected|escaped/);
});
test('invalid table-count metadata is rejected before SQL import', async () => {
  const fixture = snapshotFixture('invalid-count-metadata'); fixture.manifest.rowCounts = { 'sys_user;DROP DATABASE other': 1 }; fixture.save();
  await assert.rejects(validateSnapshot(fixture.directory, fixture.versions), /Invalid snapshot table/);
});
