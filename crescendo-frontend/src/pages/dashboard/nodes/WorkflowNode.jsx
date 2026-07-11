import { memo, useCallback } from 'react';
import { Handle, Position } from '@xyflow/react';
import { HiOutlineLightningBolt, HiOutlineCog, HiCheck, HiOutlineExclamation } from 'react-icons/hi';

/**
 * WorkflowNode — unified node for both triggers and actions.
 *
 * Connection UX:
 *   - Step 1 (first node) has ONLY an OUT handle (it's the entry point).
 *   - All other nodes have both IN and OUT handles.
 *   - Multiple edges can connect TO the IN point, and multiple can leave FROM OUT.
 *   - Horizontal layout: IN = left, OUT = right.
 *   - Vertical layout:   IN = top,  OUT = bottom.
 *   - Handles use fixed IDs ("in" / "out") so edges always resolve correctly.
 *   - Large invisible hit areas make it easy to grab the handle to start dragging.
 *   - Double-clicking a handle does NOT propagate to the node (no config panel).
 */
function WorkflowNode({ data, selected, type }) {
    const isTrigger = type === 'trigger';
    const isConfigured = !!(data.appKey && (isTrigger ? (data.triggerKey || data.actionKey) : data.actionKey));
    const appName = data.appName || data.appKey || null;
    const operationName = data.triggerName || data.actionName || null;
    const stepNumber = data.stepIndex != null ? data.stepIndex : null;
    const vertical = data._vertical || false;

    const isOrphaned = !isTrigger && !!data.isOrphaned;

    // The trigger is the sole entry point. Every action uses the same single
    // input/output pair, so branching and merging stay visually predictable.
    const hasInputHandle = !isTrigger;

    // Handle positions based on orientation
    const inPos = vertical ? Position.Top : Position.Left;
    const outPos = vertical ? Position.Bottom : Position.Right;

    // Stop double-click and click on handles from bubbling to the node
    const stopPropagation = useCallback((e) => {
        e.stopPropagation();
    }, []);

    return (
        <div className={`wf-node ${isTrigger ? 'wf-node--trigger' : 'wf-node--action'} ${selected ? 'wf-node--selected' : ''} ${isConfigured ? 'wf-node--configured' : ''} ${isOrphaned ? 'wf-node--orphaned' : ''} ${data._isNew ? 'wf-node--new' : ''}`}>
            {/* Step number badge */}
            {stepNumber != null && (
                <span className="wf-node__step-badge">{stepNumber}</span>
            )}

            <div className="wf-node__header">
                {/* App icon */}
                <div className={`wf-node__icon ${isConfigured ? 'wf-node__icon--configured' : ''}`}>
                    {(data.iconUrl || data.appKey) ? (
                        <>
                            <img 
                                src={data.iconUrl || `/icons/${data.appKey}.svg`} 
                                alt="" 
                                className="wf-node__app-img app-logo-img"
                                onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
                            />
                            {isTrigger ? <HiOutlineLightningBolt style={{ display: 'none' }} /> : <HiOutlineCog style={{ display: 'none' }} />}
                        </>
                    ) : isTrigger ? (
                        <HiOutlineLightningBolt />
                    ) : (
                        <HiOutlineCog />
                    )}
                </div>

                {/* Text */}
                <div className="wf-node__text">
                    <div className="wf-node__type-badge">
                        {isConfigured && <HiCheck className="wf-node__check" />}
                        {isTrigger ? 'Trigger' : 'Action'}
                    </div>
                    <div className="wf-node__title">
                        {data.label || (isTrigger ? 'Select Trigger' : 'Select Action')}
                    </div>
                </div>
            </div>

            {/* Body — shows app name tag, hint, or orphan warning */}
            <div className="wf-node__body">
                {isOrphaned && (
                    <div className="wf-node__orphan-warning" title="This step is not connected to the trigger — it will be skipped during execution.">
                        <HiOutlineExclamation className="wf-node__orphan-icon" />
                        <span>Not connected to trigger</span>
                    </div>
                )}
                {isConfigured ? (
                    <div className="wf-node__tags">
                        {appName && <span className="wf-node__tag">{appName}</span>}
                        {operationName && <span className="wf-node__tag wf-node__tag--op">{operationName}</span>}
                    </div>
                ) : (
                    <span className="wf-node__hint">Click to configure</span>
                )}
            </div>

            {/* IN handle — only hidden for the first node (entry point) */}
            {hasInputHandle && (
                <Handle
                    type="target"
                    position={inPos}
                    id="in"
                    className="wf-handle wf-handle--in"
                    isConnectable={true}
                    onDoubleClick={stopPropagation}
                    onClick={stopPropagation}
                />
            )}

            {/* OUT handle — always present */}
            <Handle
                type="source"
                position={outPos}
                id="out"
                className="wf-handle wf-handle--out"
                isConnectable={true}
                onDoubleClick={stopPropagation}
                onClick={stopPropagation}
            />
        </div>
    );
}

export default memo(WorkflowNode);
