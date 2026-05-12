import { memo } from 'react';
import { Handle, Position } from '@xyflow/react';
import { HiOutlineLightningBolt, HiOutlineCog, HiCheck } from 'react-icons/hi';

/**
 * WorkflowNode — unified node for both triggers and actions.
 * Renders identically except for the type badge and accent color.
 * Like Zapier, every node has: step number, app icon, type badge, operation name.
 */
function WorkflowNode({ data, selected, type }) {
    const isTrigger = type === 'trigger';
    const isConfigured = !!(data.appKey && (isTrigger ? (data.triggerKey || data.actionKey) : data.actionKey));
    const appName = data.appName || data.appKey || null;
    const operationName = data.triggerName || data.actionName || null;
    const stepNumber = data.stepIndex != null ? data.stepIndex : null;

    return (
        <div className={`wf-node ${isTrigger ? 'wf-node--trigger' : 'wf-node--action'} ${selected ? 'wf-node--selected' : ''} ${isConfigured ? 'wf-node--configured' : ''}`}>
            {/* Step number badge */}
            {stepNumber != null && (
                <span className="wf-node__step-badge">{stepNumber}</span>
            )}

            <div className="wf-node__header">
                {/* App icon */}
                <div className={`wf-node__icon ${isConfigured ? 'wf-node__icon--configured' : ''}`}>
                    {data.iconUrl ? (
                        <img src={data.iconUrl} alt="" className="wf-node__app-img" />
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

            {/* Body — shows app name tag or hint */}
            <div className="wf-node__body">
                {isConfigured ? (
                    <div className="wf-node__tags">
                        {appName && <span className="wf-node__tag">{appName}</span>}
                        {operationName && <span className="wf-node__tag wf-node__tag--op">{operationName}</span>}
                    </div>
                ) : (
                    <span className="wf-node__hint">Click to configure</span>
                )}
            </div>

            {/* Handles */}
            {isTrigger ? (
                <>
                    <Handle type="source" position={Position.Right} style={{ right: -5 }} />
                    <Handle type="source" position={Position.Bottom} id="bottom" style={{ bottom: -5 }} />
                </>
            ) : (
                <>
                    <Handle type="target" position={Position.Left} style={{ left: -5 }} />
                    <Handle type="target" position={Position.Top} id="top" style={{ top: -5 }} />
                    <Handle type="source" position={Position.Right} style={{ right: -5 }} />
                    <Handle type="source" position={Position.Bottom} id="bottom" style={{ bottom: -5 }} />
                </>
            )}
        </div>
    );
}

export default memo(WorkflowNode);
