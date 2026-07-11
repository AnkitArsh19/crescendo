package com.crescendo.emailservice.emailtemplate;

import com.crescendo.emailservice.emailtemplate.template_command.EmailTemplate_command;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates template variable completeness.
 *
 * <p>Two validation points:
 * <ol>
 *   <li><b>On publish</b>: every {@code {{VAR}}} reference in subject and HTML body must have
 *       a matching entry in the template's {@code variables} list.
 *   <li><b>On send-templated</b>: every variable whose {@code fallbackValue} is null must be
 *       supplied a runtime value in the send request.
 * </ol>
 */
@Component
public class TemplateVariableValidator {

    /** Matches {{VARIABLE_NAME}} — case-insensitive, allows spaces around the name. */
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*\\}\\}");

    /** Reserved variable names — always available at runtime without explicit declaration. */
    private static final Set<String> RESERVED = Set.of(
            "FIRST_NAME", "LAST_NAME", "EMAIL", "CRESCENDO_UNSUBSCRIBE_URL"
    );

    // ── Publish validation ───────────────────────────────────────────────────

    /**
     * Validates a template before publishing.
     * Ensures every {{VAR}} reference in subject and htmlBody has a declared variable entry
     * (or is a reserved name).
     *
     * @throws ResponseStatusException 422 if any undeclared references are found
     */
    public void validateForPublish(EmailTemplate_command template) {
        Set<String> declared = template.getVariables().stream()
                .map(v -> v.name().toUpperCase())
                .collect(Collectors.toSet());
        declared.addAll(RESERVED);

        List<String> undeclared = new ArrayList<>();
        undeclared.addAll(findUndeclared(template.getSubject(), declared));
        undeclared.addAll(findUndeclared(template.getHTMLBody(), declared));
        if (template.getTextBody() != null) {
            undeclared.addAll(findUndeclared(template.getTextBody(), declared));
        }

        if (!undeclared.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.valueOf(422),
                    "Template has undeclared variable references: " + undeclared
                    + ". Declare them in the variables list before publishing.");
        }
    }

    // ── Send-time validation ─────────────────────────────────────────────────

    /**
     * Validates that all required variables (no fallback) are supplied for a send.
     * Called by send-templated before rendering the template.
     *
     * @param template  the published template
     * @param supplied  the variable map provided by the caller
     * @throws ResponseStatusException 422 if required values are missing
     */
    public void validateForSend(EmailTemplate_command template, Map<String, Object> supplied) {
        List<String> missing = new ArrayList<>();
        for (var variable : template.getVariables()) {
            if (variable.fallbackValue() == null) {
                String key = variable.name();
                if (supplied == null || !supplied.containsKey(key)) {
                    missing.add(key);
                }
            }
        }
        if (!missing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.valueOf(422),
                    "Missing required template variables: " + missing);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Returns variable names referenced in {@code text} that are not in {@code declared}.
     */
    public List<String> findUndeclared(String text, Set<String> declared) {
        if (text == null || text.isBlank()) return List.of();
        List<String> found = new ArrayList<>();
        Matcher m = VAR_PATTERN.matcher(text);
        while (m.find()) {
            String name = m.group(1).toUpperCase();
            if (!declared.contains(name)) {
                found.add(name);
            }
        }
        return found;
    }

    /**
     * Extracts all {{VAR}} names from a text block.
     * Useful for auto-suggesting variable declarations in the editor.
     */
    public Set<String> extractVariableNames(String text) {
        if (text == null || text.isBlank()) return Set.of();
        Matcher m = VAR_PATTERN.matcher(text);
        Set<String> names = new java.util.LinkedHashSet<>();
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }
}
