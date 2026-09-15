package com.crescendo.apps.jobsearch;

import com.crescendo.apps.jobsearch.provider.AshbyProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class AshbyProviderTest {

    private final AshbyProvider provider = new AshbyProvider();

    @Test
    @DisplayName("sourceName returns Ashby")
    void sourceName_returnsAshby() {
        assertEquals("Ashby", provider.sourceName());
    }

    @Test
    @DisplayName("requiresApiKey returns false")
    void requiresApiKey_returnsFalse() {
        assertFalse(provider.requiresApiKey());
    }

    @Test
    @DisplayName("isEnabled returns true without credentials")
    void isEnabled_returnsTrue() {
        assertTrue(provider.isEnabled(Map.of()));
    }

    @Test
    @DisplayName("Internship regex accurately identifies internship keywords")
    void internPattern_matchesCorrectTitles() {
        Pattern pattern = Pattern.compile(
                "\\b(intern|internship|co-op|coop|trainee|summer\\s+intern)\\b",
                Pattern.CASE_INSENSITIVE
        );

        // Valid internship titles
        assertTrue(pattern.matcher("Software Engineering Intern").find());
        assertTrue(pattern.matcher("Research Internship (Summer 2025)").find());
        assertTrue(pattern.matcher("Hardware Co-op Student").find());
        assertTrue(pattern.matcher("Systems Engineering Coop").find());
        assertTrue(pattern.matcher("Graduate Engineering Trainee").find());
        assertTrue(pattern.matcher("Summer Intern - Deep Learning").find());

        // False positives that should NOT match
        assertFalse(pattern.matcher("Internal Communications Specialist").find());
        assertFalse(pattern.matcher("International Operations Director").find());
        assertFalse(pattern.matcher("Internet of Things Architect").find());
        assertFalse(pattern.matcher("Senior Backend Engineer").find());
    }

    @Test
    @DisplayName("matchesLocation handles fuzzy and remote matching")
    void matchesLocation_fuzzyMatching() throws Exception {
        Method method = AshbyProvider.class.getDeclaredMethod(
                "matchesLocation", String.class, String.class, boolean.class);
        method.setAccessible(true);

        // Broad searches match any location
        assertTrue((Boolean) method.invoke(provider, "India", "San Francisco, CA", false));
        assertTrue((Boolean) method.invoke(provider, "Anywhere", "London, UK", false));

        // Remote matching
        assertTrue((Boolean) method.invoke(provider, "Remote", "San Francisco, CA", true));
        assertTrue((Boolean) method.invoke(provider, "Remote", "Remote - US", false));

        // Specific city match
        assertTrue((Boolean) method.invoke(provider, "Bengaluru", "Bengaluru, Karnataka, India", false));
        assertFalse((Boolean) method.invoke(provider, "Tokyo", "London, UK", false));
    }
}
