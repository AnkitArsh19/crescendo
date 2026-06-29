package com.crescendo.shared.infrastructure.stream;

import com.crescendo.BaseIntegrationTest;
import com.crescendo.config.RedisStreamConfig;
import com.crescendo.enums.WorkflowRunStatus;
import com.crescendo.execution.engine.WorkflowExecutionEngine;
import com.crescendo.logbook.workflow_run.WorkflowRun;
import com.crescendo.logbook.workflow_run.WorkflowRunRepository;
import com.crescendo.shared.infrastructure.lock.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecutionQueueConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ExecutionQueueConsumer consumer;

    @Autowired
    private WorkflowRunRepository workflowRunRepository;

    @MockitoBean
    private DistributedLockService lockService;

    @MockitoBean
    private WorkflowExecutionEngine executionEngine;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplateMock;

    @MockitoBean
    private com.crescendo.shared.infrastructure.event.RedisStreamInitializer redisStreamInitializer;

    private WorkflowRun testRun;
    private String runIdStr;
    private String wfIdStr;

    @BeforeEach
    void setUp() {
        workflowRunRepository.deleteAll();

        testRun = new WorkflowRun();
        testRun.setId(UUID.randomUUID());
        testRun.setWorkflowId(UUID.randomUUID());
        testRun.setUserId(UUID.randomUUID());
        testRun.setStatus(WorkflowRunStatus.PENDING);
        testRun.setTriggerData(java.util.Map.of("trigger", "test"));
        testRun.setCreatedAt(Instant.now());
        
        workflowRunRepository.save(testRun);

        runIdStr = testRun.getId().toString();
        wfIdStr = testRun.getWorkflowId().toString();
    }

    @Test
    void onMessage_acquiresLock_processesAndAcks() {
        // Setup lock success
        when(lockService.tryLock(eq("workflow-execution:" + wfIdStr), anyLong()))
                .thenReturn(Optional.of("test-lock-token"));

        // Setup execution success
        doNothing().when(executionEngine).execute(any());

        // Setup Mock for redis ops
        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.StreamOperations<String, Object, Object> streamOps = mock(org.springframework.data.redis.core.StreamOperations.class);
        when(redisTemplateMock.opsForStream()).thenReturn(streamOps);

        @SuppressWarnings("unchecked")
        MapRecord<String, Object, Object> message = (MapRecord<String, Object, Object>)(MapRecord<?,?,?>)MapRecord.create(
                RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                Map.of("workflowRunId", runIdStr, "workflowId", wfIdStr, "userId", testRun.getUserId().toString())
        ).withId(RecordId.of("1-0"));

        consumer.onMessage(message);

        // Verify status changed
        WorkflowRun updated = workflowRunRepository.findById(testRun.getId()).orElseThrow();
        assertEquals(WorkflowRunStatus.RUNNING, updated.getStatus());

        // Verify engine called
        verify(executionEngine, times(1)).execute(any());

        // Verify ACK called
        verify(streamOps, times(1)).acknowledge(
                RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                RedisStreamConfig.CONSUMER_GROUP,
                RecordId.of("1-0")
        );

        // Verify unlock
        verify(lockService, times(1)).unlock(eq("workflow-execution:" + wfIdStr), eq("test-lock-token"));
    }

    @Test
    void onMessage_lockFails_doesNotAck() {
        // Setup lock failure (already in progress)
        when(lockService.tryLock(eq("workflow-execution:" + wfIdStr), anyLong()))
                .thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        org.springframework.data.redis.core.StreamOperations<String, Object, Object> streamOps = mock(org.springframework.data.redis.core.StreamOperations.class);
        when(redisTemplateMock.opsForStream()).thenReturn(streamOps);

        @SuppressWarnings("unchecked")
        MapRecord<String, Object, Object> message = (MapRecord<String, Object, Object>)(MapRecord<?,?,?>)MapRecord.create(
                RedisStreamConfig.STREAM_EXECUTION_QUEUE,
                Map.of("workflowRunId", runIdStr, "workflowId", wfIdStr)
        ).withId(RecordId.of("2-0"));

        consumer.onMessage(message);

        // Verify engine NOT called
        verify(executionEngine, never()).execute(any());

        // Verify ACK NOT called
        verify(streamOps, never()).acknowledge(anyString(), anyString(), any(RecordId.class));
    }
}
