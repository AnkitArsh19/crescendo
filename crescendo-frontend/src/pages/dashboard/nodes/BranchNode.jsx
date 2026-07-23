import { memo, useCallback } from 'react';
import { Handle, Position } from '@xyflow/react';
import { HiOutlineSwitchHorizontal, HiCheck, HiOutlineExclamation } from 'react-icons/hi';

/**
 * BranchNode — specialized canvas node for logic branching steps.
 *
 * Renders named output ports so the execution engine can route by sourceHandle:
 *   - logic:if   → two output handles: "true" and "false"
 *   - logic:switch → variable output handles: "output_0", "output_1", …, "output_N"
 *     (count driven by the `numberOutputs` configuration field, default 4)
 *
 * The single input handle always uses id="in" to match the standard node convention.
 * Output handles use the same ids that LogicHandlers emits as _branchKey values so
 * the execution engine's selectedBranch() lookup works without any extra mapping.
 *
 * Scenario D (Canvas Parity): AI-generated branching workflows use this node type
 * identically to hand-built workflows — no special AI-only rendering path exists.
 */
function BranchNode({ data, selected }) {
    const actionKey = data.actionKey || '';
    const isIf = actionKey === 'logic:if';
    const isSwitch = actionKey === 'logic:switch';

    const appName = data.appName || 'Logic';
    const operationName = data.actionName || (isIf ? 'If' : isSwitch ? 'Switch' : 'Branch');
    const stepNumber = data.stepIndex != null ? data.stepIndex : null;
    const vertical = data._vertical || false;
    const isOrphaned = !!data.isOrphaned;
    const isConfigured = !!(data.appKey && data.actionKey);

    // Input handle position
    const inPos = vertical ? Position.Top : Position.Left;
    // Output handles go on the right (horizontal) or bottom (vertical)
    const outPos = vertical ? Position.Bottom : Position.Right;

    const stopPropagation = useCallback((e) => { e.stopPropagation(); }, []);

    // ── Resolve output ports ────────────────────────────────────────────────
    const outputHandles = resolveOutputHandles(isIf, isSwitch, data.configuration);

    return (
        <div className={[
            'wf-node',
            'wf-node--branch',
            selected   ? 'wf-node--selected'   : '',
            isConfigured ? 'wf-node--configured' : '',
            isOrphaned ? 'wf-node--orphaned'   : '',
            data._isNew  ? 'wf-node--new'        : '',
        ].filter(Boolean).join(' ')}>

            {/* Step badge */}
            {stepNumber != null && (
                <span className="wf-node__step-badge">{stepNumber}</span>
            )}

            <div className="wf-node__header">
                <div className={`wf-node__icon ${isConfigured ? 'wf-node__icon--configured' : ''}`}>
                    <HiOutlineSwitchHorizontal />
                </div>
                <div className="wf-node__text">
                    <div className="wf-node__type-badge">
                        {isConfigured && <HiCheck className="wf-node__check" />}
                        Branch
                    </div>
                    <div className="wf-node__title">
                        {data.label || operationName}
                    </div>
                </div>
            </div>

            <div className="wf-node__body">
                {isOrphaned && (
                    <div className="wf-node__orphan-warning" title="Not connected to trigger — will be skipped.">
                        <HiOutlineExclamation className="wf-node__orphan-icon" />
                        <span>Not connected to trigger</span>
                    </div>
                )}
                {isConfigured ? (
                    <div className="wf-node__tags">
                        <span className="wf-node__tag">{appName}</span>
                        <span className="wf-node__tag wf-node__tag--op">{operationName}</span>
                    </div>
                ) : (
                    <span className="wf-node__hint">Click to configure</span>
                )}

                {/* Output port labels — rendered inside the node body for clarity */}
                <div className="wf-node__branch-ports">
                    {outputHandles.map((h) => (
                        <span
                            key={h.id}
                            className={`wf-node__branch-label wf-node__branch-label--${h.id.replace('_', '-')}`}
                            style={{ top: h.offsetPercent + '%' }}
                        >
                            {h.label}
                        </span>
                    ))}
                </div>
            </div>

            {/* Single IN handle */}
            <Handle
                type="target"
                position={inPos}
                id="in"
                className="wf-handle wf-handle--in"
                isConnectable
                onDoubleClick={stopPropagation}
                onClick={stopPropagation}
            />

            {/* Named output handles — one per branch */}
            {outputHandles.map((h) => (
                <Handle
                    key={h.id}
                    type="source"
                    position={outPos}
                    id={h.id}
                    style={vertical
                        ? { left: h.offsetPercent + '%', transform: 'translateX(-50%)' }
                        : { top:  h.offsetPercent + '%', transform: 'translateY(-50%)' }
                    }
                    className={`wf-handle wf-handle--out wf-handle--branch-${h.id.replace('_', '-')}`}
                    title={h.label}
                    isConnectable
                    onDoubleClick={stopPropagation}
                    onClick={stopPropagation}
                />
            ))}
        </div>
    );
}

/**
 * Builds the list of named output handle descriptors for this node.
 *
 * Each descriptor has:
 *   - id            — the sourceHandle string the engine will match (e.g. "true", "output_0")
 *   - label         — human-readable label shown next to the port
 *   - offsetPercent — vertical (or horizontal in vertical mode) offset 0–100
 *
 * @param {boolean} isIf
 * @param {boolean} isSwitch
 * @param {Object}  configuration  — the node's configuration from the canvas store
 */
function resolveOutputHandles(isIf, isSwitch, configuration) {
    if (isIf) {
        return [
            { id: 'true',  label: 'True',  offsetPercent: 35 },
            { id: 'false', label: 'False', offsetPercent: 65 },
        ];
    }

    if (isSwitch) {
        const count = parseInt(configuration?.numberOutputs ?? 4, 10) || 4;
        const step = 100 / (count + 1);
        return Array.from({ length: count }, (_, i) => ({
            id:            `output_${i}`,
            label:         `Output ${i + 1}`,
            offsetPercent: Math.round(step * (i + 1)),
        }));
    }

    // Fallback — single "out" port (same as WorkflowNode)
    return [{ id: 'out', label: 'Out', offsetPercent: 50 }];
}

export default memo(BranchNode);
