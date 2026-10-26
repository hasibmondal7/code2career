import assert from 'node:assert/strict';
import { after, before, test } from 'node:test';
import { server, validatePayload } from '../server.js';

let baseUrl;

before(async () => {
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve, reject) => server.close((error) => (
    error ? reject(error) : resolve()
  )));
});

test('validates source-code and language bounds', () => {
  assert.equal(validatePayload({
    language: 'JAVA',
    code: 'class Solution {}',
    testCases: [{ inputData: '', expectedOutput: '' }],
  }), null);
  assert.notEqual(validatePayload({
    language: 'JAVA',
    code: ' ',
    testCases: [{ inputData: '', expectedOutput: '' }],
  }), null);
});

test('rejects empty and malformed test-case collections', () => {
  assert.notEqual(validatePayload({
    language: 'JAVA',
    code: 'class Solution {}',
    testCases: [],
  }), null);
  assert.notEqual(validatePayload({
    language: 'JAVA',
    code: 'class Solution {}',
    testCases: [{ inputData: '', expectedOutput: null }],
  }), null);
});

test('health endpoint responds without invoking Docker', async () => {
  const response = await fetch(`${baseUrl}/health`);
  assert.equal(response.status, 200);
  assert.deepEqual(await response.json(), { status: 'UP' });
});

test('evaluation endpoint rejects malformed JSON', async () => {
  const response = await fetch(`${baseUrl}/evaluate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: '{',
  });
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: 'Invalid JSON request body' });
});

test('evaluation endpoint requires its internal token when configured', async () => {
  process.env.RUNNER_AUTH_TOKEN = 'test-runner-token';
  try {
    const response = await fetch(`${baseUrl}/evaluate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ language: 'JAVA', code: 'class Solution {}', testCases: [] }),
    });
    assert.equal(response.status, 401);
    assert.deepEqual(await response.json(), { error: 'Unauthorized' });
  } finally {
    delete process.env.RUNNER_AUTH_TOKEN;
  }
});

test('evaluation endpoint rejects submissions with no test cases', async () => {
  const response = await fetch(`${baseUrl}/evaluate`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ language: 'JAVA', code: 'class Solution {}', testCases: [] }),
  });
  assert.equal(response.status, 400);
  assert.deepEqual(await response.json(), { error: 'Invalid test cases' });
});
