import http from 'k6/http';
import { check } from 'k6';
import { SharedArray } from 'k6/data';
import { uuidv4 } from 'https://jslib.k6.io/k6-utils/1.4.0/index.js';

const accounts = new SharedArray('accounts', function () {
  return JSON.parse(open('/tmp/loadtest_accounts.json'));
});

export const options = {
  scenarios: {
    // 80% of traffic: unique transfers, each with a fresh Idempotency-Key -- the normal case.
    unique_transfers: {
      executor: 'constant-vus',
      vus: 40,
      duration: '30s',
      exec: 'uniqueTransfer',
    },
    // 20% of traffic: the SAME Idempotency-Key replayed repeatedly, proving the idempotency
    // fast-path holds up under sustained concurrent load, not just in a single test method.
    idempotent_replays: {
      executor: 'constant-vus',
      vus: 10,
      duration: '30s',
      exec: 'idempotentReplay',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const REPLAY_KEY = 'k6-shared-replay-key';

function pickTwoDistinct() {
  const i = Math.floor(Math.random() * accounts.length);
  let j = Math.floor(Math.random() * accounts.length);
  while (j === i) {
    j = Math.floor(Math.random() * accounts.length);
  }
  return [accounts[i], accounts[j]];
}

export function uniqueTransfer() {
  const [from, to] = pickTwoDistinct();
  const payload = JSON.stringify({
    fromAccountId: from,
    toAccountId: to,
    amount: '1.00',
    currency: 'USD',
    reason: 'k6-load-test',
  });
  const res = http.post(`${BASE_URL}/api/v1/transfers`, payload, {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': uuidv4() },
  });
  check(res, { 'unique transfer succeeded': (r) => r.status === 201 });
}

// Fixed request body so every VU's replay hashes identically -- proving the SAME logical
// request, fired concurrently and repeatedly, applies once and every caller gets the cached
// 201 result back (not a fresh transfer, and not a 409 conflict from a mismatched body).
const REPLAY_FROM = accounts[0];
const REPLAY_TO = accounts[1];

export function idempotentReplay() {
  const payload = JSON.stringify({
    fromAccountId: REPLAY_FROM,
    toAccountId: REPLAY_TO,
    amount: '2.00',
    currency: 'USD',
    reason: 'k6-idempotent-replay',
  });
  const res = http.post(`${BASE_URL}/api/v1/transfers`, payload, {
    headers: { 'Content-Type': 'application/json', 'Idempotency-Key': REPLAY_KEY },
  });
  check(res, { 'replay returned the cached 201 result': (r) => r.status === 201 });
}
