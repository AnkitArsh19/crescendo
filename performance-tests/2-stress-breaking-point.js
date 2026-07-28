import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate } from 'k6/metrics';

// Custom metrics to monitor ceiling exhaustion
const timeoutRate = new Rate('timeout_error_rate');
const poolExhaustionRate = new Rate('db_pool_exhaustion_rate');

export const options = {
    // Relentlessly increase concurrency until we uncover the system's absolute crash ceiling
    stages: [
        { duration: '30s', target: 100 },
        { duration: '1m', target: 500 },
        { duration: '1m', target: 1500 }, // Approaches laptop memory / local Tomcat limit
        { duration: '2m', target: 3000 }, // High virtual thread stress level
        { duration: '1m', target: 5000 }, // Maximum breaking stress test
        { duration: '30s', target: 0 },
    ],
    // No strict threshold aborts — the deliberate goal is to reach failure points and read Grafana logs!
};

const BASE_URL = __ENV.TARGET_URL || 'http://host.docker.internal:8080';

export default function () {
    // Stress testing: hit lightweight health/application endpoint under escalating load
    const res = http.get(`${BASE_URL}/actuator/health`, {
        timeout: '5s', // Catch stalled socket queues when Hikari pool or Tomcat maxes out
    });

    check(res, {
        'is status 200': (r) => r.status === 200,
        'not HTTP 500 or 503': (r) => r.status !== 500 && r.status !== 503,
    });

    // Record specific degradation causes
    timeoutRate.add(res.error_code === 1050 || res.timings.duration >= 5000 ? 1 : 0);
    poolExhaustionRate.add((res.body && res.body.includes('SQLTransientConnectionException')) ? 1 : 0);

    // Subtle 10ms pacing to prevent instant raw socket buffer exhaustion
    sleep(0.01);
}
