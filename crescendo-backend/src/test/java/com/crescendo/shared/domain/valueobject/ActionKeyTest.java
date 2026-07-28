package com.crescendo.shared.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActionKeyTest {

    @Test
    void acceptsNamespacedBuiltInActionKeys() {
        assertDoesNotThrow(() -> ActionKey.of("logic:if"));
        assertDoesNotThrow(() -> ActionKey.of("logic:switch"));
    }

    @Test
    void continuesToRejectUnsafeActionKeys() {
        assertThrows(IllegalArgumentException.class, () -> ActionKey.of("logic/if"));
        assertThrows(IllegalArgumentException.class, () -> ActionKey.of("1invalid"));
    }
}
