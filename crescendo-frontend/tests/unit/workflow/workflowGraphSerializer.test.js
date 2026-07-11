import { describe, expect, it } from 'vitest';
import {
    makeEdge,
    orderedNodesFromGraph,
    validateGraphForSave,
} from '../../../src/workflow/workflowGraphSerializer';

const node = (id, type, x, y) => ({
    id,
    type,
    position: { x, y },
    data: { label: id },
});

describe('workflow graph serializer', () => {
    it('accepts a DAG with multiple parents flowing into one merge action', () => {
        const nodes = [
            node('trigger', 'trigger', 0, 0),
            node('left', 'action', 200, 0),
            node('right', 'action', 200, 160),
            node('merge', 'action', 420, 80),
        ];
        const edges = [
            makeEdge('trigger', 'left'),
            makeEdge('trigger', 'right'),
            makeEdge('left', 'merge'),
            makeEdge('right', 'merge'),
        ];

        expect(validateGraphForSave(nodes, edges)).toBeNull();
        expect(orderedNodesFromGraph(nodes, edges).map((item) => item.id))
            .toEqual(['trigger', 'left', 'right', 'merge']);
    });

    it('keeps an orphan as a canvas warning case instead of blocking a valid runnable path', () => {
        const nodes = [
            node('trigger', 'trigger', 0, 0),
            node('connected', 'action', 200, 0),
            node('orphan', 'action', 200, 160),
        ];

        expect(validateGraphForSave(nodes, [makeEdge('trigger', 'connected')])).toBeNull();
    });

    it('rejects incoming connections to the trigger and cycles', () => {
        const nodes = [
            node('trigger', 'trigger', 0, 0),
            node('left', 'action', 200, 0),
            node('right', 'action', 400, 0),
        ];

        expect(validateGraphForSave(nodes, [
            makeEdge('trigger', 'left'),
            makeEdge('left', 'trigger'),
        ])).toBe('Nothing can connect into the trigger.');

        expect(validateGraphForSave(nodes, [
            makeEdge('trigger', 'left'),
            makeEdge('left', 'right'),
            makeEdge('right', 'left'),
        ])).toBe('Workflow connections cannot contain a cycle.');
    });
});
