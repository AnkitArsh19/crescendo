import { useState } from 'react';
import { BaseEdge, EdgeLabelRenderer, getSmoothStepPath } from '@xyflow/react';
import { HiPlus, HiX } from 'react-icons/hi';

/**
 * DeleteableEdge
 *
 * A custom React Flow edge that shows a small ✕ delete button
 * at the midpoint of the edge on hover or when selected.
 * Double-clicking the edge also removes it (wired in the parent).
 */
export default function DeleteableEdge({
    id,
    sourceX,
    sourceY,
    targetX,
    targetY,
    sourcePosition,
    targetPosition,
    style = {},
    markerEnd,
    data,
    selected,
}) {
    const [isHovered, setIsHovered] = useState(false);

    // In vertical layout, if handles are nearly vertically aligned (within 12px),
    // align targetX to sourceX so the connecting arrow is rendered as a clean, direct vertical line.
    // In horizontal layout, align targetY to sourceY if nearly horizontally aligned.
    const isVertical = (sourcePosition === 'bottom' && targetPosition === 'top') || (sourcePosition === 'top' && targetPosition === 'bottom');
    const isHorizontal = (sourcePosition === 'right' && targetPosition === 'left') || (sourcePosition === 'left' && targetPosition === 'right');

    const effectiveTargetX = (isVertical && Math.abs(sourceX - targetX) <= 12) ? sourceX : targetX;
    const effectiveTargetY = (isHorizontal && Math.abs(sourceY - targetY) <= 12) ? sourceY : targetY;

    const [edgePath, labelX, labelY] = getSmoothStepPath({
        sourceX,
        sourceY,
        sourcePosition,
        targetX: effectiveTargetX,
        targetY: effectiveTargetY,
        targetPosition,
        borderRadius: 12,
    });

    const onDelete = (e) => {
        e.stopPropagation();
        data?.onDelete?.(id);
    };

    const onInsert = (e) => {
        e.stopPropagation();
        data?.onInsert?.(id);
    };

    return (
        <>
            <g
                onMouseEnter={() => setIsHovered(true)}
                onMouseLeave={() => setIsHovered(false)}
            >
                <BaseEdge path={edgePath} markerEnd={markerEnd} style={style} interactionWidth={28} />
            </g>
            <EdgeLabelRenderer>
                <div
                    style={{
                        position: 'absolute',
                        transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)`,
                        pointerEvents: 'all',
                    }}
                    className="nodrag nopan"
                    onMouseEnter={() => setIsHovered(true)}
                    onMouseLeave={() => setIsHovered(false)}
                >
                    <div className={`canvas-edge-actions ${isHovered || selected ? 'is-visible' : ''}`}>
                        <button
                            className="canvas-edge-action-btn"
                            onClick={onInsert}
                            title="Insert an action in this connection"
                            aria-label="Insert an action in this connection"
                        >
                            <HiPlus />
                        </button>
                        <button
                            className="canvas-edge-action-btn canvas-edge-action-btn--delete"
                            onClick={onDelete}
                            title="Remove connection"
                            aria-label="Remove connection"
                        >
                            <HiX />
                        </button>
                    </div>
                </div>
            </EdgeLabelRenderer>
        </>
    );
}
