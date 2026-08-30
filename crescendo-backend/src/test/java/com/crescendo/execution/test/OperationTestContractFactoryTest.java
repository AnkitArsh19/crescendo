package com.crescendo.execution.test;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationTestContractFactoryTest {

    private final OperationTestContractFactory factory = new OperationTestContractFactory();

    @Test
    void writeActionWithDynamicResourcesGetsSafeTargetCheckingAndExplicitLiveWarning() {
        Map<String, Object> contract = factory.create("google-sheets", Map.of(
                "actionKey", "appendRow",
                "name", "Add Spreadsheet Row",
                "configSchema", List.of(Map.of("key", "sheetId", "label", "Sheet", "type", "dynamic_dropdown", "resourceType", "sheets"))
        ), false);

        assertEquals("READ_TARGET", contract.get("setupPolicy"));
        assertEquals("CREATE", contract.get("sideEffect"));
        assertEquals(Boolean.TRUE, contract.get("liveTestAllowed"));
        assertTrue(String.valueOf(contract.get("liveTestWarning")).contains("may change data"));
    }

    @Test
    void triggersAndLogicNeverOfferAProductionLiveRun() {
        Map<String, Object> trigger = factory.create("gmail", Map.of(
                "triggerKey", "new-email", "name", "New Email", "configSchema", List.of()), true);
        Map<String, Object> logic = factory.create("logic", Map.of(
                "actionKey", "logic:if", "name", "If", "configSchema", List.of()), false);

        assertEquals("READ_SAMPLE", trigger.get("setupPolicy"));
        assertFalse((Boolean) trigger.get("liveTestAllowed"));
        assertEquals("LOCAL_SIMULATION", logic.get("setupPolicy"));
        assertFalse((Boolean) logic.get("liveTestAllowed"));
    }

    @Test
    void readOperationsAreDeclaredNonMutating() {
        Map<String, Object> contract = factory.create("spotify", Map.of(
                "actionKey", "spotify:playlist:list", "name", "List Playlists", "configSchema", List.of()), false);

        assertEquals("READ_SAMPLE", contract.get("setupPolicy"));
        assertEquals("NONE", contract.get("sideEffect"));
    }
}
