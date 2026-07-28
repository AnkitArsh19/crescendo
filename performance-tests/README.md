# Crescendo Performance & Concurrency Verification Suite

This directory contains our comprehensive 3-tier k6 performance, stress, and concurrency benchmarking suite. It operates directly alongside our live Prometheus and Grafana telemetry stack and Java 21+ Virtual Thread implementations.

## Automated Test Execution
You can execute any of these test specifications directly using Docker from the project root without installing k6 locally:

```powershell
# 1. Load Test (Daily Peak Traffic - 200 sustained concurrent users over 3.5 minutes)
Get-Content performance-tests/1-load-test.js | docker run --rm -i loadimpact/k6 run -

# 2. Stress and Breaking-Point Test (Evaluate hardware crash ceiling up to 5,000 users)
Get-Content performance-tests/2-stress-breaking-point.js | docker run --rm -i loadimpact/k6 run -

# 3. Concurrency and Race-Condition Test (100 simultaneous workers targeting a single resource)
Get-Content performance-tests/3-concurrency-race-condition.js | docker run --rm -i loadimpact/k6 run -
```

Note: To execute against a remote staging server or alternative port, pass `-e TARGET_URL=https://your-domain.com` directly to the execution container.

---

## Live Server Telemetry & Observability
1. Start the monitoring container services: `docker-compose up -d prometheus grafana`
2. Access Grafana in your web browser at: `http://localhost:3001` (Default credentials: `admin` / `crescendo`).
3. Monitor live telemetry scraped automatically from Spring Boot Actuator (`http://localhost:8080/actuator/prometheus`):
   - **HikariCP Database Pools:** Monitor active, idle, and queued PostgreSQL connections in real time.
   - **Tomcat Throughput and Latency:** Observe p95 and p99 response time latency curves under high requests-per-second loads.
   - **JVM Virtual Thread & Heap Allocation:** Track Virtual Thread unmounting efficiency and memory stability without OS memory swapping.

---

## Explicit Race-Condition Assertion Parity
To protect against silent double-execution defects—where two simultaneous requests assume lock acquisition and silently return HTTP 200 without throwing 500 server errors or triggering visual Grafana alerts—verification is enforced across two independent layers:

1. **HTTP API Layer (`3-concurrency-race-condition.js`):** Tracks explicit quantitative metrics (`race_lock_acquired_count`, `race_clean_rejections_count`). The automated build suite terminates immediately if more than one worker reports success for the exact same resource identifier at the exact same millisecond.
2. **In-Memory JVM Layer (`DistributedLockServiceIntegrationTest.java`):** A dedicated integration test (`tryLock_under100ConcurrentVirtualThreadWorkers_exactlyOneSucceedsAnd99Fail`) synchronizes 100 parallel Virtual Threads across a simultaneous start barrier, explicitly asserting `assertEquals(1, successCount)` and `assertEquals(99, rejectedCount)`.
