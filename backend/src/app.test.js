'use strict';

const test = require('node:test');
const assert = require('node:assert/strict');

const { handler } = require('./app');

test('returns 400 when body is missing', async () => {
  const response = await handler({});
  assert.equal(response.statusCode, 400);
  assert.match(response.body, /Request body is required/);
});

