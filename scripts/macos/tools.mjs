import fs from 'node:fs';
import path from 'node:path';
import os from 'node:os';
import net from 'node:net';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';

export const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const localRoot = path.join(projectRoot, '.local/macos');
const toolsFile = path.join(localRoot, 'tools.json');

export function parseOptions(args) {
  const result = {};
  for (let i = 0; i < args.length; i += 2) {
    const key = args[i];
    if (!/^--[a-z-]+$/.test(key) || !args[i + 1] || args[i + 1].startsWith('--')) {
      throw new Error(`Expected --option VALUE, received ${key || '(empty)'}.`);
    }
    if (key in result) throw new Error(`Duplicate option ${key}.`);
    result[key] = args[i + 1];
  }
  return result;
}

function allowedOptions(options, allowed) {
  for (const name of Object.keys(options)) if (!allowed.includes(name)) throw new Error(`Unknown option ${name}.`);
}

export function assertVersion(tool, output) {
  const expected = {
    java: /(?:java|openjdk) version "1\.8\.0_231(?:["-])/, javac: /^javac 1\.8\.0_231\s*$/m,
    maven: /Apache Maven 3\.10\.0\b/, node: /^v24\.22\.1\s*$/,
    npm: /^11\.19\.1\s*$/, mysql: /\bDistrib 5\.7\.44\b/,
    redis: /\bv=5\.0\.14(?:\s|$)/,
  };
  if (!expected[tool]?.test(output)) throw new Error(`${tool} does not match the pinned baseline. No automatic upgrade or downgrade was performed.`);
}

export function validateMacVersion(version) {
  const [major, minor] = version.split('.').map(Number);
  if (!Number.isInteger(major) || !Number.isInteger(minor) || major < 13 || (major === 13 && minor < 5)) {
    throw new Error('Node 24 official binaries require macOS 13.5 or newer. Update macOS before configuring this project.');
  }
}

function capture(executable, args, env = process.env) {
  const result = spawnSync(executable, args, { encoding: 'utf8', env, cwd: projectRoot, timeout: 30000 });
  if (result.error || result.status !== 0) throw new Error(`Unable to run ${path.basename(executable)}; check the installation, executable permissions and Rosetta. No versions were changed.`);
  return `${result.stdout || ''}${result.stderr || ''}`.replace(/\x1b\[[0-9;]*m/g, '').trim();
}

function toolEnvironment(tools) {
  return { ...process.env, JAVA_HOME: tools.javaHome,
    PATH: [tools.nodeHome, tools.javaHome, tools.mavenHome, tools.mysqlHome, tools.redisHome].map(p => path.join(p, 'bin')).join(':') + ':' + (process.env.PATH || '/usr/bin:/bin:/usr/sbin:/sbin') };
}

export function generateWorkspace(template, tools, root) {
  const workspace = structuredClone(template);
  workspace.folders[0].path = '../..';
  const settings = workspace.settings;
  settings['java.configuration.runtimes'] = [{ name: 'JavaSE-1.8', path: tools.javaHome, default: true }];
  settings['java.configuration.maven.userSettings'] = path.join(root, '.mvn/settings.xml');
  settings['maven.executable.path'] = path.join(tools.mavenHome, 'bin/mvn');
  settings['terminal.integrated.env.osx'] = { JAVA_HOME: tools.javaHome,
    PATH: [tools.nodeHome, tools.javaHome, tools.mavenHome, tools.mysqlHome, tools.redisHome].map(p => path.join(p, 'bin')).join(':') + ':${env:PATH}' };
  for (const launch of workspace.launch.configurations) {
    if (launch.type === 'java') launch.javaExec = path.join(tools.javaHome, 'bin/java');
    if (launch.type === 'node') launch.runtimeExecutable = path.join(tools.nodeHome, 'bin/node');
  }
  return workspace;
}

function verifyTools(tools) {
  const env = toolEnvironment(tools);
  const probes = [
    ['java', tools.javaHome, 'java', ['-version']], ['javac', tools.javaHome, 'javac', ['-version']],
    ['maven', tools.mavenHome, 'mvn', ['-v']], ['node', tools.nodeHome, 'node', ['--version']],
    ['npm', tools.nodeHome, 'npm', ['--version']], ['mysql', tools.mysqlHome, 'mysql', ['--version']],
    ['redis', tools.redisHome, 'redis-server', ['--version']],
  ];
  for (const [name, home, executable, args] of probes) assertVersion(name, capture(path.join(home, 'bin', executable), args, env));
}

function loadTools() {
  if (!fs.existsSync(toolsFile)) throw new Error('Run bash scripts/macos/up-self.sh configure first.');
  const tools = JSON.parse(fs.readFileSync(toolsFile, 'utf8'));
  if (tools.projectRoot !== projectRoot) throw new Error('The project was moved. Re-run configure to regenerate the Mac paths.');
  return tools;
}

function requireConfig() {
  if (!fs.existsSync(path.join(projectRoot, 'config/application-local.yml'))) throw new Error('Missing config/application-local.yml. Prepare the local application configuration first.');
}

async function portOpen(port) {
  await new Promise((resolve, reject) => {
    const socket = net.createConnection({ host: '127.0.0.1', port });
    socket.setTimeout(2500);
    socket.once('connect', () => { socket.destroy(); resolve(); });
    socket.once('error', () => { socket.destroy(); reject(new Error(`No local service on port ${port}. Start MySQL and Redis before the backend.`)); });
    socket.once('timeout', () => { socket.destroy(); reject(new Error(`Local service port ${port} timed out.`)); });
  });
}

async function checkServices() {
  await Promise.all([portOpen(3306), portOpen(6379)]);
  console.log('Ports 3306 and 6379 are reachable. The backend still validates database/Redis credentials and server compatibility.');
}

async function run(executable, args, tools, cwd = projectRoot) {
  const child = spawn(executable, args, { env: toolEnvironment(tools), cwd, stdio: 'inherit' });
  const forward = signal => { if (!child.killed) child.kill(signal); };
  const interrupt = () => forward('SIGINT');
  const terminate = () => forward('SIGTERM');
  process.on('SIGINT', interrupt); process.on('SIGTERM', terminate);
  try {
    await new Promise((resolve, reject) => {
      child.once('error', reject);
      child.once('exit', (code, signal) => code === 0 ? resolve() : reject(new Error(`${path.basename(executable)} stopped (${signal || code}).`)));
    });
  } finally { process.off('SIGINT', interrupt); process.off('SIGTERM', terminate); }
}

async function main() {
  if (process.platform !== 'darwin') throw new Error('Mac commands are disabled on this operating system. Existing Windows launchers are unchanged.');
  validateMacVersion(capture('/usr/bin/sw_vers', ['-productVersion']));
  const [command = 'check', ...args] = process.argv.slice(2);
  if (command === 'configure') {
    const options = parseOptions(args);
    const names = ['java', 'maven', 'node', 'mysql', 'redis'];
    allowedOptions(options, names.map(name => `--${name}-home`));
    const tools = { projectRoot, platform: 'macOS', architecture: os.arch() };
    for (const name of names) {
      const value = options[`--${name}-home`];
      if (!value || !path.isAbsolute(value)) throw new Error(`Pass --${name}-home with its absolute installation directory (not bin).`);
      tools[`${name}Home`] = fs.realpathSync(value);
    }
    verifyTools(tools);
    const template = JSON.parse(fs.readFileSync(path.join(projectRoot, 'up-self-macos.code-workspace'), 'utf8'));
    fs.mkdirSync(localRoot, { recursive: true });
    fs.writeFileSync(toolsFile, JSON.stringify(tools, null, 2) + '\n', { mode: 0o600 });
    fs.writeFileSync(path.join(localRoot, 'node-path'), path.join(tools.nodeHome, 'bin/node') + '\n');
    fs.writeFileSync(path.join(localRoot, 'up-self.code-workspace'), JSON.stringify(generateWorkspace(template, tools, projectRoot), null, 2) + '\n');
    console.log('Pinned tool versions verified. No software, system PATH, passwords or Windows settings were changed.');
    console.log(`Open ${path.join(localRoot, 'up-self.code-workspace')}`);
    return;
  }
  const tools = loadTools();
  if (command.startsWith('db-')) {
    const { databaseAction } = await import('./database.mjs');
    await databaseAction(command, parseOptions(args), tools, projectRoot);
    return;
  }
  if (args.length) throw new Error(`Unexpected arguments for ${command}.`);
  const maven = lifecycle => run(path.join(tools.mavenHome, 'bin/mvn'), ['-B', `-Dmaven.repo.local=${path.join(projectRoot, '.local/maven-repository')}`, lifecycle], tools);
  const npm = script => run(path.join(tools.nodeHome, 'bin/npm'), script, tools, path.join(projectRoot, 'frontend'));
  switch (command) {
    case 'check': verifyTools(tools); console.log('All pinned Mac tool versions match. Service authentication is checked by the backend/db-check.'); break;
    case 'check-services': await checkServices(); break;
    case 'install': verifyTools(tools); await npm(['ci']); break;
    case 'build': verifyTools(tools); await maven('package'); await npm(['run', 'build']); break;
    case 'prepare-backend': verifyTools(tools); requireConfig(); await checkServices(); await maven('process-resources'); break;
    case 'backend': {
      verifyTools(tools); requireConfig(); await checkServices();
      const jar = path.join(projectRoot, 'up-self-admin/target/up-self-admin.jar');
      if (!fs.existsSync(jar)) throw new Error('The backend jar is missing. Run the Mac build command first.');
      await run(path.join(tools.javaHome, 'bin/java'), ['-Dfile.encoding=UTF-8', '-jar', jar, '--spring.config.additional-location=file:./config/'], tools); break;
    }
    case 'frontend': await npm(['run', 'dev']); break;
    case 'redis': {
      assertVersion('redis', capture(path.join(tools.redisHome, 'bin/redis-server'), ['--version']));
      const conf = path.join(localRoot, 'redis.conf');
      if (!fs.existsSync(conf)) throw new Error('Prepare .local/macos/redis.conf following the Mac guide before starting Redis.');
      await run(path.join(tools.redisHome, 'bin/redis-server'), [conf], tools); break;
    }
    case 'open-vscode': await run('/usr/bin/open', ['-a', 'Visual Studio Code', path.join(localRoot, 'up-self.code-workspace')], tools); break;
    default: throw new Error(`Unknown Mac command ${command}. Run the shell launcher with help.`);
  }
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  main().catch(error => { console.error(error.message); process.exitCode = 1; });
}
