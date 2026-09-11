import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { mkdir, rm, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { spawn } from 'node:child_process';

const port = Number(process.env.PORT || 8090);
const workspace = process.env.RUNNER_WORKSPACE || '/workspace';
const dockerVolume = process.env.RUNNER_DOCKER_VOLUME || 'code2career_runner_workspace';
const image = process.env.RUNNER_IMAGE || 'eclipse-temurin:17-alpine';

const send = (response, status, body) => {
  response.writeHead(status, { 'Content-Type': 'application/json' });
  response.end(JSON.stringify(body));
};

const run = (args, timeoutMs) => new Promise((resolve) => {
  const child = spawn('docker', args, { stdio: ['ignore', 'pipe', 'pipe'] });
  let stdout = '';
  let stderr = '';
  child.stdout.on('data', (chunk) => { stdout += chunk; });
  child.stderr.on('data', (chunk) => { stderr += chunk; });
  const timer = setTimeout(() => {
    child.kill('SIGKILL');
    resolve({ timedOut: true, code: -1, stdout, stderr });
  }, timeoutMs);
  child.on('close', (code) => {
    clearTimeout(timer);
    resolve({ timedOut: false, code, stdout, stderr });
  });
});

const execute = async (request) => {
  const job = randomUUID();
  const jobPath = join(workspace, job);
  await mkdir(jobPath, { recursive: true });
  try {
    await writeFile(join(jobPath, 'Solution.java'), request.code, 'utf8');
    const compile = await run([
      'run', '--rm', '--network', 'none', '--memory=256m', '--cpus=1',
      '--pids-limit=64', '--cap-drop=ALL', '--security-opt=no-new-privileges',
      '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
      '-v', `${dockerVolume}:/app`, '-w', `/app/${job}`, image,
      'javac', 'Solution.java'
    ], 15000);
    if (compile.timedOut || compile.code !== 0) {
      return { status: 'COMPILATION_ERROR', executionTimeMs: 0, error: compile.stderr };
    }

    if (typeof request.customInput === 'string') {
      await writeFile(join(jobPath, 'input.txt'), request.customInput, 'utf8');
      const result = await run([
        'run', '--rm', '--network', 'none', '--memory=256m', '--cpus=1',
        '--pids-limit=64', '--cap-drop=ALL', '--security-opt=no-new-privileges',
        '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
        '-v', `${dockerVolume}:/app`, '-w', `/app/${job}`, image,
        'sh', '-c', 'java Solution < input.txt'
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
        'run', '--rm', '--network', 'none', '--memory=256m', '--cpus=1',
        '--pids-limit=64', '--cap-drop=ALL', '--security-opt=no-new-privileges',
        '--read-only', '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m',
        '-v', `${dockerVolume}:/app`, '-w', `/app/${job}`, image,
        'sh', '-c', 'java Solution < input.txt'
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
