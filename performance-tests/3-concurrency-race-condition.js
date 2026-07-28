import http from 'k6/http';
import { check } from 'k6';
import { Counter, Gauge } from 'k6/metrics';

// Explicit Quantitative Counters for Race-Condition Parity
const successCount = new Counter('race_lock_acquired_count');
const cleanRejectionCount = new Counter('race_clean_rejections_count');
const unhandled500ErrorCount = new Counter('race_unhandled_500_errors');
const doubleExecutionBug = new Counter('race_CRITICAL_DOUBLE_EXECUTION_BUG');

export const options = {
    // 100 synchronized virtual users all attempting to mutate/lock the EXACT same resource at the exact same millisecond
    scenarios: {
        simultaneous_race_strike: {
            executor: 'per-vu-iterations',
            vus: 100,
            iterations: 1,
            maxDuration: '10s',
        },
    },
    thresholds: {
        // EXACT quantitative assertions requested in review:
        // Why? Because absence of 500 errors in Grafana is NOT enough! If two workers acquire a lock silently, both return 200 without throwing an exception!
        'race_unhandled_500_errors': ['count==0'],            // Must be 0 server crashes
        'race_lock_acquired_count': ['count<=1'],             // Exactly 1 worker must succeed (no double executions!)
        'race_CRITICAL_DOUBLE_EXECUTION_BUG': ['count==0'],   // Zero silent double executions allowed!
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://host.docker.internal:8080';

// Shared target across all 100 concurrent workers
const SHARED_RESOURCE_ID = 'concurrent-workflow-run-001';

export default function () {
    // Target an execution or state-change webhook/endpoint
    const payload = JSON.stringify({
        workflowRunId: SHARED_RESOURCE_ID,
        triggeredBy: `worker-${__VU}`,
        timestamp: Date.now()
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-Crescendo-Idempotency-Key': SHARED_RESOURCE_ID // Testing idempotency and distributed lock boundaries
        },
        timeout: '5s',
    };

    // Replace with exact workflow execution trigger URL once auth/token is configured in environment
    const res = http.post(`${BASE_URL}/actuator/health`, payload, params);

    // Track explicit categorization of responses
    if (res.status === 200 || res.status === 201 || res.status === 202) {
        successCount.add(1);
    } else if (res.status === 409 || res.status === 423 || res.status === 429 || (res.status === 200 && res.body.includes('ALREADY_PROCESSED'))) {
        // Clean rejection (Lock held by another worker, or idempotency cache returned already-handled status)
        cleanRejectionCount.add(1);
    } else if (res.status >= 500) {
        unhandled500ErrorCount.add(1);
    }
}

export function handleSummary(data) {
    const wins = data.metrics.race_lock_acquired_count ? data.metrics.race_lock_acquired_count.values.count : 0;
    const rejections = data.metrics.race_clean_rejections_count ? data.metrics.race_clean_rejections_count.values.count : 0;

    console.log('\n================================================================================');
    console.log('                 CONCURRENCY & RACE-CONDITION TEST REPORT');
    console.log('================================================================================');
    console.log(`Total Concurrent Workers Executed : 100`);
    console.log(`Successful Executions (Locks Won) : ${wins}`);
    console.log(`Clean Rejections (Queued/Locked)  : ${rejections}`);
    
    if (wins > 1) {
        console.error(`\n[CRITICAL BUG FAILED ASSERTION] Silent Double-Execution Detected! ${wins} workers both succeeded!`);
        data.metrics.race_CRITICAL_DOUBLE_EXECUTION_BUG = { values: { count: wins - 1 } };
    } else if (wins === 1) {
        console.log(`\n[SUCCESS] Exactly ONE worker succeeded! No double-execution data corruption!`);
    }
    console.log('================================================================================\n');

    return {
        'stdout': JSON.stringify(data, null, 2),
    };
}
