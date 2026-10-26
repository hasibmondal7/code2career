import http from 'node:http';
import { randomUUID } from 'node:crypto';
import { mkdir, rm, chmod, writeFile } from 'node:fs/promises';
import { join } from 'node:path';
import { spawn } from 'node:child_process';
import { pathToFileURL } from 'node:url';

const port = Number(process.env.PORT || 8090);
const workspace = process.env.RUNNER_WORKSPACE || '/workspace';
const dockerVolume = process.env.RUNNER_DOCKER_VOLUME || 'code2career_runner_workspace';
const useHostWorkspace = process.env.RUNNER_HOST_WORKSPACE === 'true'
  || /^[A-Za-z]:[\\/]/.test(workspace);
const MAX_REQUEST_BYTES = 16 * 1024 * 1024;
const MAX_CODE_LENGTH = 100_000;
const MAX_FIELD_LENGTH = 100_000;
const MAX_TEST_CASES = 100;
const MAX_OUTPUT_BYTES = 1024 * 1024;
const MAX_EVALUATION_TIME_MS = 60_000;
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

const validatePayload = (payload) => {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)
      || typeof payload.language !== 'string' || !payload.language.trim()
      || payload.language.length > 30
      || typeof payload.code !== 'string' || !payload.code.trim()
      || payload.code.length > MAX_CODE_LENGTH) {
    return 'Invalid language or code';
  }
  if (payload.customInput !== undefined && payload.customInput !== null
      && (typeof payload.customInput !== 'string' || payload.customInput.length > MAX_FIELD_LENGTH)) {
    return 'Invalid custom input';
  }
  if (payload.customInput == null) {
    if (!Array.isArray(payload.testCases) || payload.testCases.length === 0
        || payload.testCases.length > MAX_TEST_CASES) {
      return 'Invalid test cases';
    }
    for (const testCase of payload.testCases) {
      if (!testCase || typeof testCase !== 'object'
          || typeof testCase.inputData !== 'string'
          || typeof testCase.expectedOutput !== 'string'
          || testCase.inputData.length > MAX_FIELD_LENGTH
          || testCase.expectedOutput.length > MAX_FIELD_LENGTH) {
        return 'Invalid test case';
      }
    }
  }
  return null;
};

const cleanupContainer = (containerName) => new Promise((resolve) => {
  const child = spawn('docker', ['rm', '--force', containerName], { stdio: 'ignore' });
  const timer = setTimeout(() => {
    child.kill('SIGKILL');
    resolve();
  }, 3000);
  child.once('error', () => {
    clearTimeout(timer);
    resolve();
  });
  child.once('close', () => {
    clearTimeout(timer);
    resolve();
  });
});

const run = (args, timeoutMs, containerName) => new Promise((resolve) => {
  const child = spawn('docker', args, { stdio: ['ignore', 'pipe', 'pipe'] });
  const stdout = [];
  const stderr = [];
  let stdoutBytes = 0;
  let stderrBytes = 0;
  let outputLimitExceeded = false;
  let systemError = false;
  let timedOut = false;
  let settled = false;

  const collect = (target, chunk, bytes) => {
    const remaining = MAX_OUTPUT_BYTES - bytes;
    if (remaining > 0) {
      target.push(chunk.subarray(0, remaining));
    }
    return bytes + chunk.length;
  };

  child.stdout.on('data', (chunk) => {
    stdoutBytes = collect(stdout, chunk, stdoutBytes);
    if (stdoutBytes > MAX_OUTPUT_BYTES && !outputLimitExceeded) {
      outputLimitExceeded = true;
      child.kill('SIGKILL');
    }
  });
  child.stderr.on('data', (chunk) => {
    stderrBytes = collect(stderr, chunk, stderrBytes);
    if (stderrBytes > MAX_OUTPUT_BYTES && !outputLimitExceeded) {
      outputLimitExceeded = true;
      child.kill('SIGKILL');
    }
  });

  const timer = setTimeout(() => {
    timedOut = true;
    child.kill('SIGKILL');
  }, timeoutMs);

  child.once('error', (error) => {
    if (settled) return;
    settled = true;
    systemError = true;
    clearTimeout(timer);
    resolve({
      timedOut: false,
      outputLimitExceeded: false,
      systemError,
      code: -1,
      stdout: Buffer.concat(stdout).toString('utf8'),
      stderr: `${Buffer.concat(stderr).toString('utf8')}${error.message}`,
    });
  });
  child.once('close', async (code) => {
    if (settled) return;
    settled = true;
    clearTimeout(timer);
    if (timedOut || outputLimitExceeded) {
      await cleanupContainer(containerName);
    }
    resolve({
      timedOut,
      outputLimitExceeded,
      systemError: code === 125,
      code,
      stdout: Buffer.concat(stdout).toString('utf8'),
      stderr: Buffer.concat(stderr).toString('utf8'),
    });
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
    : ['--mount', `type=volume,source=${dockerVolume},target=/app,volume-subpath=${job}`];
  const sandboxOptions = [
    '--network=none',
    '--memory=256m',
    '--cpus=1',
    '--pids-limit=64',
    '--cap-drop=ALL',
    '--security-opt=no-new-privileges',
    '--read-only',
    '--user=10001:10001',
    '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m,uid=10001,gid=10001',
    ...appMountArgs,
    '-w', '/app',
  ];
  let containerIndex = 0;
  const nextContainerName = () => `c2c-${job.slice(0, 8)}-${containerIndex++}`;
  const startedAt = Date.now();

  await mkdir(jobPath, { recursive: true, mode: 0o777 });
  await chmod(jobPath, 0o777);
  try {
    await writeFile(join(jobPath, target.file), request.code, 'utf8');
    if (target.compile) {
      const containerName = nextContainerName();
      const compile = await run([
        'run', '--rm', '--name', containerName, ...sandboxOptions, target.image, ...target.compile,
      ], 15_000, containerName);
      if (compile.systemError) {
        return { status: 'SYSTEM_ERROR', executionTimeMs: 0 };
      }
      if (compile.timedOut || compile.outputLimitExceeded || compile.code !== 0) {
        return {
          status: 'COMPILATION_ERROR',
          executionTimeMs: 0,
          error: compile.outputLimitExceeded ? 'Compiler output exceeded the limit' : compile.stderr,
        };
      }
    }

    if (typeof request.customInput === 'string') {
      await writeFile(join(jobPath, 'input.txt'), request.customInput, 'utf8');
      const containerName = nextContainerName();
      const result = await run([
        'run', '--rm', '--name', containerName, ...sandboxOptions, target.image,
        'sh', '-c', `${target.execute} < input.txt`,
      ], 5000, containerName);
      if (result.timedOut) {
        return { status: 'TIME_LIMIT_EXCEEDED', executionTimeMs: 0 };
      }
      if (result.systemError) {
        return { status: 'SYSTEM_ERROR', executionTimeMs: 0 };
      }
      if (result.outputLimitExceeded) {
        return { status: 'RUNTIME_ERROR', executionTimeMs: 0, error: 'Program output exceeded the limit' };
      }
      if (result.code !== 0) {
        return { status: 'RUNTIME_ERROR', executionTimeMs: 0, error: result.stderr };
      }
      return { status: 'ACCEPTED', executionTimeMs: 0, output: result.stdout.trim() };
    }

    let maxExecutionTimeMs = 0;
    for (const testCase of request.testCases) {
      const remainingTimeMs = MAX_EVALUATION_TIME_MS - (Date.now() - startedAt);
      if (remainingTimeMs <= 0) {
        return { status: 'TIME_LIMIT_EXCEEDED', executionTimeMs: maxExecutionTimeMs };
      }
      await writeFile(join(jobPath, 'input.txt'), testCase.inputData, 'utf8');
      const started = Date.now();
      const containerName = nextContainerName();
      const result = await run([
        'run', '--rm', '--name', containerName, ...sandboxOptions, target.image,
        'sh', '-c', `${target.execute} < input.txt`,
      ], Math.min(3000, remainingTimeMs), containerName);
      maxExecutionTimeMs = Math.max(maxExecutionTimeMs, Date.now() - started);
      if (result.timedOut) {
        return { status: 'TIME_LIMIT_EXCEEDED', executionTimeMs: maxExecutionTimeMs };
      }
      if (result.systemError) {
        return { status: 'SYSTEM_ERROR', executionTimeMs: maxExecutionTimeMs };
      }
      if (result.outputLimitExceeded) {
        return {
          status: 'RUNTIME_ERROR',
          executionTimeMs: maxExecutionTimeMs,
          error: 'Program output exceeded the limit',
        };
      }
      if (result.code !== 0) {
        return { status: 'RUNTIME_ERROR', executionTimeMs: maxExecutionTimeMs, error: result.stderr };
      }
      if (result.stdout.trim() !== testCase.expectedOutput.trim()) {
        return { status: 'WRONG_ANSWER', executionTimeMs: maxExecutionTimeMs };
      }
    }
    return { status: 'ACCEPTED', executionTimeMs: maxExecutionTimeMs };
  } finally {
    await rm(jobPath, { recursive: true, force: true });
  }
};

const readPayload = (request, response) => new Promise((resolve) => {
  const chunks = [];
  let bodyBytes = 0;
  let oversized = false;
  request.on('data', (chunk) => {
    bodyBytes += chunk.length;
    if (bodyBytes > MAX_REQUEST_BYTES) {
      oversized = true;
      chunks.length = 0;
      return;
    }
    if (!oversized) chunks.push(chunk);
  });
  request.once('end', () => {
    if (oversized) {
      send(response, 413, { error: 'Request body is too large' });
      resolve(null);
      return;
    }
    try {
      resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')));
    } catch {
      send(response, 400, { error: 'Invalid JSON request body' });
      resolve(null);
    }
  });
  request.once('error', () => {
    if (!response.headersSent) {
      send(response, 400, { error: 'Unable to read request body' });
    }
    resolve(null);
  });
});

const server = http.createServer(async (request, response) => {
  if (request.method === 'GET' && request.url === '/health') {
    send(response, 200, { status: 'UP' });
    return;
  }
  if (request.method !== 'POST' || request.url !== '/evaluate') {
    send(response, 404, { error: 'Not found' });
    return;
  }
  const runnerToken = process.env.RUNNER_AUTH_TOKEN || '';
  if (runnerToken && request.headers['x-code-runner-token'] !== runnerToken) {
    send(response, 401, { error: 'Unauthorized' });
    return;
  }
  if (!request.headers['content-type']?.toLowerCase().startsWith('application/json')) {
    send(response, 415, { error: 'Content-Type must be application/json' });
    return;
  }
  if (Number(request.headers['content-length']) > MAX_REQUEST_BYTES) {
    request.resume();
    send(response, 413, { error: 'Request body is too large' });
    return;
  }

  const payload = await readPayload(request, response);
  if (payload === null) return;
  const validationError = validatePayload(payload);
  if (validationError) {
    send(response, 400, { error: validationError });
    return;
  }

  try {
    send(response, 200, await execute(payload));
  } catch (error) {
    console.error('Evaluation failed:', error);
    send(response, 500, {
      status: 'SYSTEM_ERROR',
      executionTimeMs: 0,
      error: 'Code evaluation is temporarily unavailable',
    });
  }
});

server.requestTimeout = 15_000;

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  server.listen(port, '0.0.0.0', () => {
    console.log(`Code runner listening on port ${port}`);
  });
}

export { server, validatePayload };
