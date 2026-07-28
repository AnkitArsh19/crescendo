/**
 * workflowGraphSerializer.test.js
 *
 * Unit tests for workflowGraphSerializer.js — the single source of truth for
 * graph <-> step-DTO conversion in the Crescendo frontend.
 */

import { describe, it, expect } from "vitest";
import {
    resolveNodeType,
    stepsToGraph,
    makeEdge,
    nodeToStepPayload,
    edgesToPayload,
    parseConfigSchema,
    toPersistedConfig,
    orderedNodesFromGraph,
    validateGraphForSave,
    validateNodeForSave,
} from "../../../src/workflow/workflowGraphSerializer.js";

// ─── Helpers ──────────────────────────────────────────────────────────────────

/** Build a minimal backend step DTO. */
function makeStep(overrides = {}) {
    return {
        id: overrides.id ?? "step-1",
        name: overrides.name ?? "My Step",
        type: overrides.type ?? "ACTION",
        stepType: overrides.stepType ?? overrides.type ?? "ACTION",
        appKey: overrides.appKey ?? "gmail",
        actionKey: overrides.actionKey ?? "send_email",
        connectionId: overrides.connectionId ?? "conn-1",
        configuration: overrides.configuration ?? {},
        order: overrides.order ?? 0,
        ...overrides,
    };
}

/** Build a minimal React Flow node. */
function makeNode(overrides = {}) {
    return {
        id: overrides.id ?? "node-1",
        type: overrides.type ?? "action",
        position: overrides.position ?? { x: 0, y: 0 },
        data: {
            appKey: "gmail",
            actionKey: "send_email",
            connectionId: "conn-1",
            configuration: {},
            label: "Send Email",
            _backendId: "step-1",
            ...(overrides.data ?? {}),
        },
    };
}

/** Build a React Flow edge. */
function makeRFEdge(source, target, sourceHandle = "out", targetHandle = "in") {
    return { id: `e${source}-${target}`, source, target, sourceHandle, targetHandle };
}

// ─── resolveNodeType ──────────────────────────────────────────────────────────

describe("resolveNodeType", () => {
    it("returns trigger for a TRIGGER stepType", () => {
        expect(resolveNodeType({ stepType: "TRIGGER", appKey: "gmail", actionKey: "new_email" })).toBe("trigger");
    });

    it("returns branch for logic:if", () => {
        expect(resolveNodeType({ stepType: "ACTION", appKey: "logic", actionKey: "logic:if" })).toBe("branch");
    });

    it("returns branch for logic:switch", () => {
        expect(resolveNodeType({ stepType: "ACTION", appKey: "logic", actionKey: "logic:switch" })).toBe("branch");
    });

    it("returns action for logic:merge (not a branch node)", () => {
        expect(resolveNodeType({ stepType: "ACTION", appKey: "logic", actionKey: "logic:merge" })).toBe("action");
    });

    it("returns action for a regular action step", () => {
        expect(resolveNodeType({ stepType: "ACTION", appKey: "slack", actionKey: "post_message" })).toBe("action");
    });

    it("returns action for null/undefined step", () => {
        expect(resolveNodeType(null)).toBe("action");
        expect(resolveNodeType(undefined)).toBe("action");
    });
});

// ─── stepsToGraph ─────────────────────────────────────────────────────────────

describe("stepsToGraph", () => {
    it("single trigger produces one node and no edges (fallback linear chain)", () => {
        const trigger = makeStep({ id: "t1", type: "TRIGGER", stepType: "TRIGGER", order: 0 });
        const { nodes, edges } = stepsToGraph([trigger]);
        expect(nodes).toHaveLength(1);
        expect(edges).toHaveLength(0);
    });

    it("trigger + action produces one linear edge out->in when no backendEdges", () => {
        const trigger = makeStep({ id: "t1", type: "TRIGGER", stepType: "TRIGGER", order: 0 });
        const action = makeStep({ id: "a1", type: "ACTION", stepType: "ACTION", order: 1 });
        const { edges } = stepsToGraph([trigger, action]);
        expect(edges).toHaveLength(1);
        expect(edges[0].source).toBe("t1");
        expect(edges[0].target).toBe("a1");
        expect(edges[0].sourceHandle).toBe("out");
        expect(edges[0].targetHandle).toBe("in");
    });

    it("backendEdges override the linear fallback", () => {
        const trigger = makeStep({ id: "t1", type: "TRIGGER", stepType: "TRIGGER", order: 0 });
        const a1 = makeStep({ id: "a1", type: "ACTION", stepType: "ACTION", order: 1 });
        const a2 = makeStep({ id: "a2", type: "ACTION", stepType: "ACTION", order: 2 });
        const backendEdges = [
            { sourceStepId: "t1", targetStepId: "a2", sourceHandle: "true", targetHandle: "in" },
        ];
        const { edges } = stepsToGraph([trigger, a1, a2], backendEdges);
        expect(edges).toHaveLength(1);
        expect(edges[0].source).toBe("t1");
        expect(edges[0].target).toBe("a2");
        expect(edges[0].sourceHandle).toBe("true");
    });

    it("backendEdges with unknown stepId are filtered out", () => {
        const trigger = makeStep({ id: "t1", type: "TRIGGER", stepType: "TRIGGER", order: 0 });
        const backendEdges = [
            { sourceStepId: "t1", targetStepId: "NONEXISTENT", sourceHandle: "out", targetHandle: "in" },
        ];
        const { edges } = stepsToGraph([trigger], backendEdges);
        expect(edges).toHaveLength(0);
    });

    it("nodes carry _backendId, connectionId, and configuration from step DTO", () => {
        const step = makeStep({ id: "a1", connectionId: "conn-42", configuration: { subject: "Hello" } });
        const { nodes } = stepsToGraph([step]);
        const node = nodes[0];
        expect(node.data._backendId).toBe("a1");
        expect(node.data.connectionId).toBe("conn-42");
        expect(node.data.configuration).toEqual({ subject: "Hello" });
    });

    it("logic:if step produces a node of type branch", () => {
        const step = makeStep({ id: "b1", appKey: "logic", actionKey: "logic:if", type: "ACTION", stepType: "ACTION" });
        const { nodes } = stepsToGraph([step]);
        expect(nodes[0].type).toBe("branch");
    });

    it("steps are sorted by order before layout regardless of input order", () => {
        const s1 = makeStep({ id: "first", order: 0 });
        const s2 = makeStep({ id: "second", order: 1 });
        const { nodes } = stepsToGraph([s2, s1]);
        expect(nodes[0].id).toBe("first");
        expect(nodes[1].id).toBe("second");
    });

    it("triggerKey set on trigger nodes, actionName set on action nodes", () => {
        const trigger = makeStep({ id: "t1", type: "TRIGGER", stepType: "TRIGGER", name: "New Email", actionKey: "new_email", order: 0 });
        const action = makeStep({ id: "a1", type: "ACTION", stepType: "ACTION", name: "Send Message", actionKey: "send_message", order: 1 });
        const { nodes } = stepsToGraph([trigger, action]);
        expect(nodes[0].data.triggerKey).toBe("new_email");
        expect(nodes[0].data.actionName).toBeUndefined();
        expect(nodes[1].data.triggerKey).toBeUndefined();
        expect(nodes[1].data.actionName).toBe("Send Message");
    });
});

// ─── makeEdge ─────────────────────────────────────────────────────────────────

describe("makeEdge", () => {
    it("builds a valid React Flow edge with defaults", () => {
        const edge = makeEdge("A", "B");
        expect(edge.source).toBe("A");
        expect(edge.target).toBe("B");
        expect(edge.sourceHandle).toBe("out");
        expect(edge.targetHandle).toBe("in");
        expect(edge.type).toBe("deleteable");
    });

    it("preserves custom source and target handles", () => {
        const edge = makeEdge("A", "B", "true", "in");
        expect(edge.sourceHandle).toBe("true");
    });

    it("generates a unique id from source and target", () => {
        const e1 = makeEdge("X", "Y");
        const e2 = makeEdge("Y", "X");
        expect(e1.id).not.toBe(e2.id);
    });
});

// ─── edgesToPayload ───────────────────────────────────────────────────────────

describe("edgesToPayload", () => {
    it("maps React Flow edges to EdgeRequest format", () => {
        const edges = [makeRFEdge("A", "B", "true", "in")];
        const payload = edgesToPayload(edges);
        expect(payload).toHaveLength(1);
        expect(payload[0].clientSourceId).toBe("A");
        expect(payload[0].clientTargetId).toBe("B");
        expect(payload[0].sourceHandle).toBe("true");
        expect(payload[0].targetHandle).toBe("in");
    });

    it("returns empty array for null/undefined input", () => {
        expect(edgesToPayload(null)).toEqual([]);
        expect(edgesToPayload(undefined)).toEqual([]);
    });

    it("uses null for missing handles", () => {
        const edges = [{ id: "e1", source: "A", target: "B" }];
        const payload = edgesToPayload(edges);
        expect(payload[0].sourceHandle).toBeNull();
        expect(payload[0].targetHandle).toBeNull();
    });
});

// ─── parseConfigSchema ────────────────────────────────────────────────────────

describe("parseConfigSchema", () => {
    it("returns empty array for null/undefined", () => {
        expect(parseConfigSchema(null)).toEqual([]);
        expect(parseConfigSchema(undefined)).toEqual([]);
    });

    it("parses structured array format correctly", () => {
        const schema = [
            { key: "to", label: "To Email", type: "text", required: true },
            { key: "subject", label: "Subject", type: "text", required: false },
        ];
        const fields = parseConfigSchema(schema);
        expect(fields).toHaveLength(2);
        expect(fields[0].key).toBe("to");
        expect(fields[0].required).toBe(true);
        expect(fields[1].required).toBe(false);
    });

    it("defaults type to text and required to false when omitted", () => {
        const fields = parseConfigSchema([{ key: "myField" }]);
        expect(fields[0].type).toBe("text");
        expect(fields[0].required).toBe(false);
    });

    it("parses legacy object format (hint strings)", () => {
        const schema = { email: "Required. The target email address." };
        const fields = parseConfigSchema(schema);
        expect(fields).toHaveLength(1);
        expect(fields[0].key).toBe("email");
        expect(fields[0].required).toBe(true);
    });

    it("detects number type from legacy hint string", () => {
        const fields = parseConfigSchema({ count: "A number representing the count." });
        expect(fields[0].type).toBe("number");
    });

    it("normalises dependsOn to array from string", () => {
        const fields = parseConfigSchema([{ key: "col", dependsOn: "sheet" }]);
        expect(fields[0].dependsOn).toEqual(["sheet"]);
    });

    it("preserves dependsOn when already an array", () => {
        const fields = parseConfigSchema([{ key: "col", dependsOn: ["sheet", "spreadsheet"] }]);
        expect(fields[0].dependsOn).toEqual(["sheet", "spreadsheet"]);
    });

    it("preserves options array on dropdown fields", () => {
        const schema = [{ key: "format", type: "dropdown", options: [{ value: "csv", label: "CSV" }] }];
        const fields = parseConfigSchema(schema);
        expect(fields[0].options).toHaveLength(1);
        expect(fields[0].options[0].value).toBe("csv");
    });
});

// ─── toPersistedConfig ────────────────────────────────────────────────────────

describe("toPersistedConfig", () => {
    it("casts number string to number", () => {
        const fields = [{ key: "limit", type: "number" }];
        const result = toPersistedConfig(fields, { limit: "10" });
        expect(result.limit).toBe(10);
        expect(typeof result.limit).toBe("number");
    });

    it("casts true string to boolean true", () => {
        const fields = [{ key: "enabled", type: "boolean" }];
        expect(toPersistedConfig(fields, { enabled: "true" }).enabled).toBe(true);
    });

    it("casts false string to boolean false", () => {
        const fields = [{ key: "enabled", type: "boolean" }];
        expect(toPersistedConfig(fields, { enabled: "false" }).enabled).toBe(false);
    });

    it("splits comma-separated string to array", () => {
        const fields = [{ key: "tags", type: "array" }];
        expect(toPersistedConfig(fields, { tags: "a, b, c" }).tags).toEqual(["a", "b", "c"]);
    });

    it("parses JSON string for json type", () => {
        const fields = [{ key: "meta", type: "json" }];
        expect(toPersistedConfig(fields, { meta: '{"key":"value"}' }).meta).toEqual({ key: "value" });
    });

    it("leaves text fields as-is", () => {
        const fields = [{ key: "subject", type: "text" }];
        expect(toPersistedConfig(fields, { subject: "Hello World" }).subject).toBe("Hello World");
    });

    it("leaves null/empty values unchanged", () => {
        const fields = [{ key: "limit", type: "number" }];
        expect(toPersistedConfig(fields, { limit: "" }).limit).toBe("");
    });
});

// ─── nodeToStepPayload ────────────────────────────────────────────────────────

describe("nodeToStepPayload", () => {
    const appDetails = {
        gmail: {
            actions: [
                { actionKey: "send_email", configSchema: [{ key: "to", type: "text", required: true }] },
            ],
            triggers: [],
        },
    };

    it("returns null when appKey is missing", () => {
        const node = makeNode({ data: { appKey: null, actionKey: "send_email", connectionId: "c1" } });
        expect(nodeToStepPayload(node, appDetails)).toBeNull();
    });

    it("returns null when actionKey is missing", () => {
        const node = makeNode({ data: { appKey: "gmail", actionKey: null, connectionId: "c1" } });
        expect(nodeToStepPayload(node, appDetails)).toBeNull();
    });

    it("maps action node to correct step DTO shape", () => {
        const node = makeNode({
            type: "action",
            data: { appKey: "gmail", actionKey: "send_email", connectionId: "conn-1", configuration: { to: "a@b.com" }, label: "Send", _backendId: "step-99" },
        });
        const payload = nodeToStepPayload(node, appDetails);
        expect(payload.type).toBe("ACTION");
        expect(payload.appKey).toBe("gmail");
        expect(payload.actionKey).toBe("send_email");
        expect(payload.connectionId).toBe("conn-1");
        expect(payload.configuration.to).toBe("a@b.com");
        expect(payload.backendId).toBe("step-99");
    });

    it("maps trigger node using triggerKey and TRIGGER type", () => {
        const gmailTrigger = {
            gmail: { triggers: [{ triggerKey: "new_email", configSchema: [] }], actions: [] },
        };
        const node = {
            id: "n1", type: "trigger", position: { x: 0, y: 0 },
            data: { appKey: "gmail", triggerKey: "new_email", actionKey: undefined, connectionId: "c1", configuration: {}, label: "New Email", _backendId: null },
        };
        const payload = nodeToStepPayload(node, gmailTrigger);
        expect(payload.type).toBe("TRIGGER");
        expect(payload.actionKey).toBe("new_email");
    });
});

// ─── orderedNodesFromGraph ────────────────────────────────────────────────────

describe("orderedNodesFromGraph", () => {
    it("returns empty array for empty nodes", () => {
        expect(orderedNodesFromGraph([], [])).toEqual([]);
    });

    it("places trigger first in a linear graph regardless of input order", () => {
        const trigger = makeNode({ id: "t1", type: "trigger", position: { x: 0, y: 0 } });
        const action = makeNode({ id: "a1", type: "action", position: { x: 330, y: 0 } });
        const edges = [makeRFEdge("t1", "a1")];
        const ordered = orderedNodesFromGraph([action, trigger], edges);
        expect(ordered[0].id).toBe("t1");
        expect(ordered[1].id).toBe("a1");
    });

    it("includes orphan nodes at the end without crashing", () => {
        const trigger = makeNode({ id: "t1", type: "trigger", position: { x: 0, y: 0 } });
        const action = makeNode({ id: "a1", type: "action", position: { x: 330, y: 0 } });
        const orphan = makeNode({ id: "orphan", type: "action", position: { x: 0, y: 400 } });
        const edges = [makeRFEdge("t1", "a1")];
        const ordered = orderedNodesFromGraph([trigger, action, orphan], edges);
        expect(ordered).toHaveLength(3);
        expect(ordered.map((n) => n.id)).toContain("orphan");
    });
});

// ─── validateGraphForSave ─────────────────────────────────────────────────────

describe("validateGraphForSave", () => {
    const trigger = makeNode({ id: "t1", type: "trigger", position: { x: 0, y: 0 } });
    const action = makeNode({ id: "a1", type: "action", position: { x: 330, y: 0 } });

    it("returns error when there is no trigger", () => {
        expect(validateGraphForSave([action], [])).toMatch(/trigger/i);
    });

    it("returns error when there are fewer than 2 nodes", () => {
        expect(validateGraphForSave([trigger], [])).toMatch(/action/i);
    });

    it("returns error when trigger has an incoming edge", () => {
        const edges = [makeRFEdge("a1", "t1")];
        expect(validateGraphForSave([trigger, action], edges)).toMatch(/trigger/i);
    });

    it("returns error for a self-loop", () => {
        const edges = [makeRFEdge("a1", "a1")];
        expect(validateGraphForSave([trigger, action], edges)).toMatch(/itself/i);
    });

    it("returns error for a cycle", () => {
        const n1 = makeNode({ id: "n1", type: "action", position: { x: 330, y: 0 } });
        const n2 = makeNode({ id: "n2", type: "action", position: { x: 660, y: 0 } });
        const edges = [makeRFEdge("t1", "n1"), makeRFEdge("n1", "n2"), makeRFEdge("n2", "n1")];
        expect(validateGraphForSave([trigger, n1, n2], edges)).toMatch(/cycle/i);
    });

    it("returns error for duplicate connection", () => {
        const edges = [makeRFEdge("t1", "a1"), makeRFEdge("t1", "a1")];
        expect(validateGraphForSave([trigger, action], edges)).toMatch(/already exists/i);
    });

    it("returns null for a valid linear DAG", () => {
        const edges = [makeRFEdge("t1", "a1")];
        expect(validateGraphForSave([trigger, action], edges)).toBeNull();
    });
});

// ─── validateNodeForSave ──────────────────────────────────────────────────────

describe("validateNodeForSave", () => {
    const catalogApps = [
        { appKey: "gmail", authType: "OAUTH2" },
        { appKey: "webhook", authType: "NONE" },
    ];
    const appDetails = {
        gmail: {
            actions: [
                { actionKey: "send_email", configSchema: [{ key: "to", label: "To", type: "text", required: true }] },
            ],
            triggers: [],
        },
    };

    it("returns error when connectionId missing for app that needs auth", () => {
        const node = makeNode({ type: "action", data: { appKey: "gmail", actionKey: "send_email", connectionId: null, configuration: { to: "x@y.com" } } });
        expect(validateNodeForSave(node, 1, catalogApps, appDetails)).toMatch(/connected account/i);
    });

    it("returns error when required config field is missing", () => {
        const node = makeNode({ type: "action", data: { appKey: "gmail", actionKey: "send_email", connectionId: "c1", configuration: {} } });
        expect(validateNodeForSave(node, 1, catalogApps, appDetails)).toMatch(/To/i);
    });

    it("returns error when appKey is missing", () => {
        const node = makeNode({ type: "action", data: { appKey: null, actionKey: "send_email", connectionId: "c1", configuration: {} } });
        expect(validateNodeForSave(node, 1, catalogApps, appDetails)).toMatch(/select an app/i);
    });

    it("returns null for a fully configured node", () => {
        const node = makeNode({ type: "action", data: { appKey: "gmail", actionKey: "send_email", connectionId: "c1", configuration: { to: "a@b.com" } } });
        expect(validateNodeForSave(node, 1, catalogApps, appDetails)).toBeNull();
    });
});
