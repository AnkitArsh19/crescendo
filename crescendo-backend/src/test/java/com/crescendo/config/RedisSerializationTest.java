package com.crescendo.config;

import com.crescendo.workflow.WorkflowDto;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class RedisSerializationTest {

    private ObjectMapper redisObjectMapper() {
        return JsonMapper.builder()
                .activateDefaultTyping(
                        BasicPolymorphicTypeValidator.builder()
                                .allowIfBaseType(Object.class)
                                .allowIfSubType("com.crescendo")
                                .allowIfSubType("java.util.")
                                .allowIfSubType("java.time.")
                                .allowIfSubType("java.math.")
                                .allowIfSubType("java.lang.")
                                .build(),
                        DefaultTyping.NON_FINAL_AND_RECORDS,
                        JsonTypeInfo.As.PROPERTY
                )
                .build();
    }

    @Test
    public void testWorkflowDetailSerialization() throws Exception {
        ObjectMapper mapper = redisObjectMapper();

        Map<String, Object> config = new HashMap<>();
        config.put("text", "hello");
        Map<String, Object> nested = new HashMap<>();
        nested.put("key", "val");
        config.put("nested", nested);
        config.put("list", List.of("a", "b"));

        WorkflowDto.StepResponse step = new WorkflowDto.StepResponse(
                UUID.randomUUID().toString(),
                "Step 1",
                "ACTION",
                BigDecimal.ONE,
                "agent",
                "ai_agent",
                null,
                config,
                Instant.now(),
                Instant.now()
        );

        WorkflowDto.WorkflowDetailResponse detail = new WorkflowDto.WorkflowDetailResponse(
                UUID.randomUUID().toString(),
                "My Workflow",
                "desc",
                true,
                "ACTIVE",
                1L,
                List.of(step),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now()
        );

        String json = mapper.writeValueAsString(detail);
        System.out.println("SERIALIZED JSON: " + json);

        Object deserialized = mapper.readValue(json, Object.class);
        assertNotNull(deserialized);
    }
}
