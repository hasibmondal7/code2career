import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { spawn } from 'node:child_process';

const port = Number(process.env.PORT || 8090);
const workspace = process.env.RUNNER_WORKSPACE || '/workspace';
const dockerVolume = process.env.RUNNER_DOCKER_VOLUME || 'code2career_runner_workspace';
const useHostWorkspace = process.env.RUNNER_HOST_WORKSPACE === 'true'
  || /^[A-Za-z]:[\\/]/.test(workspace);
const executionTargets = {
  JAVA: {
    image: process.env.RUNNER_JAVA_IMAGE || 'eclipse-temurin:17-alpine',
    file: 'Solution.java',
    compile: ['javac', 'Solution.java'],
    execute: 'java Solution',
  },
  CPP: {
    image: process.env.RUNNER_CPP_IMAGE || 'gcc:14',
    file: 'solution.cpp',
    compile: ['g++', '-std=c++17', '-O2', 'solution.cpp', '-o', 'solution'],
    execute: './solution',
  },
  PYTHON: {
    image: process.env.RUNNER_PYTHON_IMAGE || 'python:3.13-alpine',
    file: 'solution.py',
    compile: null,
    execute: 'python3 solution.py',
  },
  JAVASCRIPT: {
    image: process.env.RUNNER_JAVASCRIPT_IMAGE || 'node:22-alpine',
    file: 'solution.js',
    compile: null,
    execute: 'node solution.js',
  },
};

const normalizeLanguage = (language) => {
  const normalized = String(language || '').trim().toUpperCase();
  if (normalized === 'C++' || normalized === 'CXX') return 'CPP';
  if (normalized === 'JS' || normalized === 'NODE') return 'JAVASCRIPT';
  if (normalized === 'PYTHON3' || normalized === 'PY') return 'PYTHON';
  return normalized;
};

const send = (response, status, body) => {
  response.writeHead(status, { 'Content-Type': 'application/json' });
  response.end(JSON.stringify(body));
};

const run = (args, timeoutMs) => new Promise((resolve) => {
  const child = spawn('docker', args, { stdio: ['ignore', 'pipe', 'pipe'] });
  let stdout = '';
  let stderr = '';
  let settled = false;
  child.stdout.on('data', (chunk) => { stdout += chunk; });
  child.stderr.on('data', (chunk) => { stderr += chunk; });
  const timer = setTimeout(() => {
    child.kill('SIGKILL');
    settled = true;
    resolve({ timedOut: true, code: -1, stdout, stderr });
  }, timeoutMs);
  child.on('error', (error) => {
    if (settled) return;
    settled = true;
    clearTimeout(timer);
    resolve({ timedOut: false, code: -1, stdout, stderr: `${stderr}${error.message}` });
  });
  child.on('close', (code) => {
    if (settled) return;
    settled = true;
    clearTimeout(timer);
    resolve({ timedOut: false, code, stdout, stderr });
  });
});

const execute = async (request) => {
  const target = executionTargets[normalizeLanguage(request.language)];
  if (!target) {
    return { status: 'COMPILATION_ERROR', executionTimeMs: 0, error: 'Unsupported language' };
  }
  const job = randomUUID();
  const jobPath = join(workspace, job);
  const appMountArgs = useHostWorkspace
    ? ['--mount', `type=bind,source=${jobPath},target=/app`]
    : ['-v', `${dockerVolume}:/app`];
  const containerWorkdir = useHostWorkspace ? '/app' : `/app/${job}`;
  await mkdir(jobPath, { recursive: true });
  try {
    await writeFile(join(jobPath, target.file), request.code, 'utf8');
    const sandboxOptions = [
      '--network', 'none', '--memory=256m', '--cpus=1',
      '--pids-limit=64', '--cap-drop=ALL', '--security-opt=no-new-privileges',
      '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
      ...appMountArgs, '-w', containerWorkdir,
    ];
    if (target.compile) {
      const compile = await run([
      'run', '--rm', '--network', 'none', '--memory=256m', '--cpus=1',
      '--pids-limit=64', '--cap-drop=ALL', '--security-opt=no-new-privileges',
      '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
      ...appMountArgs, '-w', containerWorkdir, target.image,
      ...target.compile
      ], 15000);
      if (compile.timedOut || compile.code !== 0) {
        return { status: 'COMPILATION_ERROR', executionTimeMs: 0, error: compile.stderr };
      }
    }

    if (typeof request.customInput === 'string') {
      await writeFile(join(jobPath, 'input.txt'), request.customInput, 'utf8');
      const result = await run([
        'run', '--rm', ...sandboxOptions, target.image,
        'sh', '-c', `${target.execute} < input.txt`
      ], 5000);
      if (result.timedOut) {
        return { status: 'TIME_LIMIT_EXCEEDED', executionTimeMs: 0 };
      }
      if (result.code !== 0) {
        return { status: 'RUNTIME_ERROR', executionTimeMs: 0, error: result.stderr };
      }
      return { status: 'ACCEPTED', executionTimeMs: 0, output: result.stdout.trim() };
    }

    let maxExecutionTimeMs = 0;
    for (const testCase of request.testCases || []) {
      await writeFile(join(jobPath, 'input.txt'), testCase.inputData || '', 'utf8');
      const started = Date.now();
      const result = await run([
        'run', '--rm', ...sandboxOptions, target.image,
        'sh', '-c', `${target.execute} < input.txt`
      ], 3000);
      maxExecutionTimeMs = Math.max(maxExecutionTimeMs, Date.now() - started);
      if (result.timedOut) {
        return { status: 'TIME_LIMIT_EXCEEDED', executionTimeMs: maxExecutionTimeMs };
      }
      if (result.code !== 0) {
        return { status: 'RUNTIME_ERROR', executionTimeMs: maxExecutionTimeMs, error: result.stderr };
      }
      if (result.stdout.trim() !== String(testCase.expectedOutput || '').trim()) {
        return { status: 'WRONG_ANSWER', executionTimeMs: maxExecutionTimeMs };
      }
    }
    return { status: 'ACCEPTED', executionTimeMs: maxExecutionTimeMs };
  } finally {
    await rm(jobPath, { recursive: true, force: true });
  }
};

const server = http.createServer(async (request, response) => {
  if (request.method === 'GET' && request.url === '/health') {
    send(response, 200, { status: 'UP' });
    return;
  }
  if (request.method !== 'POST' || request.url !== '/evaluate') {
    send(response, 404, { error: 'Not found' });
    return;
  }
  let body = '';
  request.on('data', (chunk) => { body += chunk; });
  request.on('end', async () => {
    try {
      const payload = JSON.parse(body);
      if (typeof payload.code !== 'string' || payload.code.length > 100_000) {
        send(response, 400, { error: 'Invalid code payload' });
        return;
      }
      send(response, 200, await execute(payload));
    } catch (error) {
      send(response, 500, { status: 'SYSTEM_ERROR', executionTimeMs: 0, error: error.message });
    }
  });
});

server.listen(port, '0.0.0.0', () => {
  console.log(`Code runner listening on port ${port}`);
});
