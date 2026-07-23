package com.crescendo.apps.logic;

import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionMapping;
import com.crescendo.execution.action.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Handlers for logic-and-flow nodes: If and Switch.
 *
 * <h3>If node</h3>
 * Evaluates a structured {@code conditions} array (groups of AND/OR condition triples)
 * and emits {@code _branchKey} = {@code "true"} or {@code "false"}.
 * The execution engine uses {@code _branchKey} to route edges via {@code sourceHandle}.
 *
 * <h3>Switch node</h3>
 * Supports two modes:
 * <ul>
 *   <li>{@code mode = "rules"} (default) — evaluates each rule (value/operator/matchValue triples)
 *       and routes to the first matching {@code outputIndex}.</li>
 *   <li>{@code mode = "expression"} — uses the {@code output} config field directly as
 *       the zero-based output index.</li>
 * </ul>
 *
 * <h3>Supported operators (both nodes)</h3>
 * equals, notEquals, contains, notContains, startsWith, endsWith,
 * isEmpty, isNotEmpty, greaterThan, lessThan, greaterThanOrEqual,
 * lessThanOrEqual, regex.
 */
@Component
public class LogicHandlers {

    private static final Logger log = LoggerFactory.getLogger(LogicHandlers.class);

    // ─── If ────────────────────────────────────────────────────────────────────

    /**
     * Evaluates the {@code conditions} config field and returns a result map
     * containing {@code _branchKey}: either {@code "true"} or {@code "false"}.
     *
     * <p>Expected {@code conditions} shape (matches InternalCatalogController shapeHint):
     * <pre>
     * [
     *   {
     *     "combinator": "AND",           // "AND" or "OR"
     *     "conditions": [
     *       { "leftValue": "{{steps.1.subject}}", "operator": "contains", "rightValue": "urgent" }
     *     ]
     *   }
     * ]
     * </pre>
     * Multiple groups are combined with OR (any group passing = overall true).
     */
    @ActionMapping(appKey = "logic", actionKey = "logic:if")
    public Object ifNode(ActionContext context) {
        Object rawConditions = context.get("conditions");

        if (!(rawConditions instanceof List<?> conditions) || conditions.isEmpty()) {
            return ActionResult.failure("An If branch requires at least one condition group.");
        }

        boolean result = evaluateConditionGroups(rawConditions, context);
        String branchKey = result ? "true" : "false";

        log.info("[logic:if] step={} evaluated to branch='{}'", context.stepId(), branchKey);

        return Map.of(
            "_branchKey", branchKey,
            "branch",     branchKey,
            "status",     "success"
        );
    }

    // ─── Switch ────────────────────────────────────────────────────────────────

    /**
     * Routes to an output index based on {@code mode}:
     * <ul>
     *   <li>{@code "rules"} (default): first matching rule wins.</li>
     *   <li>{@code "expression"}: reads {@code output} config field directly.</li>
     * </ul>
     *
     * <p>Returns a map containing {@code _branchKey} = {@code "output_N"} (0-indexed)
     * so the execution engine can route via {@code sourceHandle}.
     * The canvas must render output ports named {@code "output_0"}, {@code "output_1"}, etc.
     */
    @ActionMapping(appKey = "logic", actionKey = "logic:switch")
    public Object switchNode(ActionContext context) {
        String mode = context.getString("mode");
        if (mode == null || mode.isBlank()) mode = "rules";
        if (!"rules".equalsIgnoreCase(mode) && !"expression".equalsIgnoreCase(mode)) {
            return ActionResult.failure("Switch mode must be 'rules' or 'expression'.");
        }

        int numberOutputs = context.getInt("numberOutputs", 2);
        if (numberOutputs < 2 || numberOutputs > 32) {
            return ActionResult.failure("Switch numberOutputs must be between 2 and 32.");
        }

        int outputIndex;

        if ("expression".equalsIgnoreCase(mode)) {
            // Direct index mode
            Object raw = context.get("output");
            outputIndex = toInt(raw, 0);
        } else {
            // Rules mode — evaluate each rule and take the first match
            Object rawRules = context.get("rules");
            outputIndex = evaluateSwitchRules(rawRules, context, fallbackOutput(context));
        }

        if (outputIndex < 0 || outputIndex >= numberOutputs) {
            return ActionResult.failure("Switch selected output " + outputIndex
                    + " but numberOutputs is " + numberOutputs + ".");
        }

        String branchKey = "output_" + outputIndex;
        log.info("[logic:switch] step={} routed to '{}'", context.stepId(), branchKey);

        return Map.of(
            "_branchKey",  branchKey,
            "outputIndex", outputIndex,
            "status",      "success"
        );
    }

    @ActionMapping(appKey = "logic", actionKey = "logic:merge")
    public Object mergeNode(ActionContext context) {
        return context.input();
    }

    // ─── Condition group evaluation ────────────────────────────────────────────

    /**
     * Evaluates a top-level conditions array.
     * Multiple groups are combined with OR (any group passing = true).
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateConditionGroups(Object rawConditions, ActionContext context) {
        if (!(rawConditions instanceof List<?> groups) || groups.isEmpty()) {
            log.warn("[logic:if] No conditions configured — defaulting to true");
            return true;
        }

        for (Object g : groups) {
            if (!(g instanceof Map<?, ?> group)) continue;
            Object combinatorValue = group.get("combinator");
            String combinator = String.valueOf(combinatorValue != null ? combinatorValue : "AND").toUpperCase();
            Object rawConds = group.get("conditions");

            if (!(rawConds instanceof List<?> conds)) continue;

            boolean groupResult;
            if ("OR".equals(combinator)) {
                groupResult = conds.stream()
                    .filter(c -> c instanceof Map<?, ?>)
                    .map(c -> evaluateSingleCondition((Map<String, Object>) c))
                    .anyMatch(Boolean::booleanValue);
            } else { // AND
                groupResult = conds.stream()
                    .filter(c -> c instanceof Map<?, ?>)
                    .map(c -> evaluateSingleCondition((Map<String, Object>) c))
                    .allMatch(Boolean::booleanValue);
            }

            if (groupResult) return true; // OR across groups
        }
        return false;
    }

    /**
     * Evaluates a single condition triple:
     * {@code { "leftValue": "...", "operator": "...", "rightValue": "..." }}.
     */
    @SuppressWarnings("unchecked")
    private boolean evaluateSingleCondition(Map<String, Object> cond) {
        String left = String.valueOf(cond.getOrDefault("leftValue", ""));
        String operator = String.valueOf(cond.getOrDefault("operator", "equals")).toLowerCase();
        String right = String.valueOf(cond.getOrDefault("rightValue", ""));
        return applyOperator(left, operator, right);
    }

    // ─── Switch rule evaluation ────────────────────────────────────────────────

    /**
     * Evaluates an array of switch rules.
     * Returns the {@code outputIndex} of the first matching rule, or 0 as fallback.
     */
    @SuppressWarnings("unchecked")
    private int evaluateSwitchRules(Object rawRules, ActionContext context, int fallbackOutput) {
        if (!(rawRules instanceof List<?> rules) || rules.isEmpty()) return fallbackOutput;

        for (Object r : rules) {
            if (!(r instanceof Map<?, ?> rule)) continue;
            Object rawValue = rule.get("value");
            Object rawOperator = rule.get("operator");
            Object rawMatchValue = rule.get("matchValue");
            String value      = String.valueOf(rawValue != null ? rawValue : "");
            String operator   = String.valueOf(rawOperator != null ? rawOperator : "equals").toLowerCase();
            String matchValue = String.valueOf(rawMatchValue != null ? rawMatchValue : "");
            int    outIdx     = toInt(rule.get("outputIndex"), 0);

            if (applyOperator(value, operator, matchValue)) {
                log.debug("[logic:switch] Rule matched: '{}' {} '{}' -> output {}", value, operator, matchValue, outIdx);
                return outIdx;
            }
        }

        log.debug("[logic:switch] No rules matched — defaulting to output 0");
        return fallbackOutput;
    }

    // ─── Operator engine ───────────────────────────────────────────────────────

    /**
     * Applies a named operator to two string values.
     *
     * <p>Supported operators: equals, notEquals, contains, notContains,
     * startsWith, endsWith, isEmpty, isNotEmpty, greaterThan, lessThan,
     * greaterThanOrEqual, lessThanOrEqual, regex.
     */
    private int fallbackOutput(ActionContext context) {
        return toInt(context.getMap("options").get("fallbackOutput"), 0);
    }

    private boolean applyOperator(String left, String operator, String right) {
        if (left == null) left = "";
        if (right == null) right = "";

        return switch (operator) {
            case "equals"             -> left.equals(right);
            case "notequals",
                 "notequal",
                 "neq"                -> !left.equals(right);
            case "contains"           -> left.contains(right);
            case "notcontains",
                 "doesnotcontain"     -> !left.contains(right);
            case "startswith"         -> left.startsWith(right);
            case "endswith"           -> left.endsWith(right);
            case "isempty",
                 "empty"              -> left.trim().isEmpty();
            case "isnotempty",
                 "notempty"           -> !left.trim().isEmpty();
            case "greaterthan",
                 "gt"                 -> numericComparison(left, right, comparison -> comparison > 0);
            case "lessthan",
                 "lt"                 -> numericComparison(left, right, comparison -> comparison < 0);
            case "greaterthanorequal",
                 "gte"                -> numericComparison(left, right, comparison -> comparison >= 0);
            case "lessthanorequal",
                 "lte"                -> numericComparison(left, right, comparison -> comparison <= 0);
            case "regex"              -> {
                if (right.length() > 256 || left.length() > 10_000) yield false;
                try { yield Pattern.compile(right).matcher(left).find(); }
                catch (Exception e) {
                    log.warn("[logic] Invalid regex '{}': {}", right, e.getMessage());
                    yield false;
                }
            }
            default -> {
                log.warn("[logic] Unknown operator '{}' — treating as false", operator);
                yield false;
            }
        };
    }

    private boolean numericComparison(String a, String b, java.util.function.IntPredicate predicate) {
        try {
            double da = Double.parseDouble(a.trim());
            double db = Double.parseDouble(b.trim());
            return predicate.test(Double.compare(da, db));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(String.valueOf(value).trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}

