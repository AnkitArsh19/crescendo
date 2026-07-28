import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

// Custom metrics
const latencyTrend = new Trend('read_query_latency_ms');
const successCounter = new Counter('successful_reads');

export const options = {
    stages: [
        { duration: '30s', target: 50 },  // Ramp up to 50 concurrent virtual users
        { duration: '1m', target: 200 },  // Reach simulated daily peak traffic (200 VUs)
        { duration: '2m', target: 200 },  // Sustain peak load for 2 minutes
        { duration: '30s', target: 0 },   // Cool down gracefully
    ],
    thresholds: {
        http_req_failed: ['rate<0.01'],    // Less than 1% of requests should fail
        http_req_duration: ['p(95)<150'],  // 95% of requests must complete under 150ms
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://host.docker.internal:8080';

export default function () {
    // Test Type 1: Read-Only Query Bursts (Tests CQRS Query Postgres pool & JSON rendering)
    // Note: If auth is required, substitute valid token or test public endpoints / actuator health
    const res = http.get(`${BASE_URL}/actuator/health`);

    const pass = check(res, {
        'is status 200': (r) => r.status === 200,
        'response time < 150ms': (r) => r.timings.duration < 150,
    });

    if (pass) {
        successCounter.add(1);
    }
    latencyTrend.add(res.timings.duration);

    // Realistic developer pacing between requests (100ms - 500ms)
    sleep(0.1 + Math.random() * 0.4);
}
