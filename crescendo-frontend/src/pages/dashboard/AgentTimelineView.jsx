import { useState } from 'react';
import {
  HiOutlineSparkles,
  HiOutlineLightningBolt,
  HiOutlineDocumentText,
  HiOutlineCheckCircle,
  HiOutlineExclamationCircle,
  HiOutlineChevronDown,
  HiOutlineChevronUp,
  HiOutlineClipboardCopy,
  HiCheck,
} from 'react-icons/hi';
import './AgentTimelineView.css';

export default function AgentTimelineView({ timeline = [], iterations = 0, tokensUsed = 0, status = 'COMPLETED' }) {
  const [expandedTurns, setExpandedTurns] = useState({});
  const [copiedIndex, setCopiedIndex] = useState(null);

  if (!timeline || !Array.isArray(timeline) || timeline.length === 0) {
    return null;
  }

  const toggleExpand = (idx) => {
    setExpandedTurns((prev) => ({
      ...prev,
      [idx]: !prev[idx],
    }));
  };

  const handleCopy = (idx, text) => {
    if (!text) return;
    const toCopy = typeof text === 'object' ? JSON.stringify(text, null, 2) : String(text);
    navigator.clipboard.writeText(toCopy).catch(() => {});
    setCopiedIndex(idx);
    setTimeout(() => setCopiedIndex(null), 2000);
  };

  const toolCount = timeline.filter((t) => t.type === 'tool_call').length;

  return (
    <div className="agent-timeline-wrapper">
      <div className="agent-timeline-header">
        <div className="agent-timeline-title-group">
          <span className="agent-timeline-icon"><HiOutlineSparkles /></span>
          <div>
            <h4 className="agent-timeline-title">Autonomous ReAct Timeline</h4>
            <p className="agent-timeline-subtitle">Step-by-step reasoning, tool dispatch, and observation trace</p>
          </div>
        </div>
        <div className="agent-timeline-stats">
          <span className="agent-timeline-badge turns">{iterations || 1} {iterations === 1 ? 'Turn' : 'Turns'}</span>
          {toolCount > 0 && (
            <span className="agent-timeline-badge tools">{toolCount} {toolCount === 1 ? 'Tool Call' : 'Tool Calls'}</span>
          )}
          {tokensUsed > 0 && (
            <span className="agent-timeline-badge tokens">{tokensUsed} Tokens</span>
          )}
          <span className={`agent-timeline-badge status ${status.toLowerCase()}`}>{status}</span>
        </div>
      </div>

      <div className="agent-timeline-list">
        {timeline.map((event, idx) => {
          const isExpanded = expandedTurns[idx] ?? true; // Default expanded for readability
          const isCopied = copiedIndex === idx;

          if (event.type === 'input') {
            return (
              <div key={idx} className="agent-timeline-card input">
                <div className="agent-timeline-card-header" onClick={() => toggleExpand(idx)}>
                  <div className="agent-timeline-card-meta">
                    <span className="agent-timeline-node-icon input"><HiOutlineDocumentText /></span>
                    <span className="agent-timeline-tag">User Input</span>
                    <span className="agent-timeline-turn">Turn 0</span>
                  </div>
                  <div className="agent-timeline-card-controls">
                    <button
                      type="button"
                      className="agent-timeline-copy-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(idx, event.content);
                      }}
                      title="Copy input payload"
                    >
                      {isCopied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                    </button>
                    <button type="button" className="agent-timeline-toggle-btn">
                      {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="agent-timeline-card-body">
                    <pre className="agent-timeline-code">{typeof event.content === 'object' ? JSON.stringify(event.content, null, 2) : event.content}</pre>
                  </div>
                )}
              </div>
            );
          }

          if (event.type === 'thought') {
            return (
              <div key={idx} className="agent-timeline-card thought">
                <div className="agent-timeline-card-header" onClick={() => toggleExpand(idx)}>
                  <div className="agent-timeline-card-meta">
                    <span className="agent-timeline-node-icon thought"><HiOutlineSparkles /></span>
                    <span className="agent-timeline-tag">Reasoning / Thought</span>
                    <span className="agent-timeline-turn">Turn {event.turn}</span>
                  </div>
                  <div className="agent-timeline-card-controls">
                    <button
                      type="button"
                      className="agent-timeline-copy-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(idx, event.thought);
                      }}
                      title="Copy reasoning"
                    >
                      {isCopied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                    </button>
                    <button type="button" className="agent-timeline-toggle-btn">
                      {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="agent-timeline-card-body">
                    <p className="agent-timeline-thought-text">{event.thought}</p>
                  </div>
                )}
              </div>
            );
          }

          if (event.type === 'tool_call') {
            return (
              <div key={idx} className="agent-timeline-card tool-call">
                <div className="agent-timeline-card-header" onClick={() => toggleExpand(idx)}>
                  <div className="agent-timeline-card-meta">
                    <span className="agent-timeline-node-icon tool-call"><HiOutlineLightningBolt /></span>
                    <span className="agent-timeline-tag">Tool Dispatched</span>
                    <span className="agent-timeline-app-pill">{event.appKey || 'app'}:{event.actionKey || event.toolId}</span>
                    <span className="agent-timeline-turn">Turn {event.turn}</span>
                  </div>
                  <div className="agent-timeline-card-controls">
                    <button
                      type="button"
                      className="agent-timeline-copy-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(idx, event.arguments);
                      }}
                      title="Copy tool arguments"
                    >
                      {isCopied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                    </button>
                    <button type="button" className="agent-timeline-toggle-btn">
                      {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="agent-timeline-card-body">
                    <div className="agent-timeline-sublabel">Arguments passed to tool:</div>
                    <pre className="agent-timeline-code">{JSON.stringify(event.arguments || {}, null, 2)}</pre>
                  </div>
                )}
              </div>
            );
          }

          if (event.type === 'observation') {
            return (
              <div key={idx} className="agent-timeline-card observation">
                <div className="agent-timeline-card-header" onClick={() => toggleExpand(idx)}>
                  <div className="agent-timeline-card-meta">
                    <span className="agent-timeline-node-icon observation"><HiOutlineCheckCircle /></span>
                    <span className="agent-timeline-tag">Observation / Tool Output</span>
                    <span className="agent-timeline-turn">Turn {event.turn}</span>
                  </div>
                  <div className="agent-timeline-card-controls">
                    <button
                      type="button"
                      className="agent-timeline-copy-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(idx, event.output);
                      }}
                      title="Copy tool observation"
                    >
                      {isCopied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                    </button>
                    <button type="button" className="agent-timeline-toggle-btn">
                      {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="agent-timeline-card-body">
                    <div className="agent-timeline-sublabel">Result returned to agent:</div>
                    <pre className="agent-timeline-code">{JSON.stringify(event.output || {}, null, 2)}</pre>
                  </div>
                )}
              </div>
            );
          }

          if (event.type === 'final_answer') {
            return (
              <div key={idx} className="agent-timeline-card final-answer">
                <div className="agent-timeline-card-header" onClick={() => toggleExpand(idx)}>
                  <div className="agent-timeline-card-meta">
                    <span className="agent-timeline-node-icon final-answer"><HiOutlineSparkles /></span>
                    <span className="agent-timeline-tag">Final Answer</span>
                    <span className="agent-timeline-turn">Turn {event.turn}</span>
                  </div>
                  <div className="agent-timeline-card-controls">
                    <button
                      type="button"
                      className="agent-timeline-copy-btn"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopy(idx, event.answer);
                      }}
                      title="Copy final answer"
                    >
                      {isCopied ? <HiCheck /> : <HiOutlineClipboardCopy />}
                    </button>
                    <button type="button" className="agent-timeline-toggle-btn">
                      {isExpanded ? <HiOutlineChevronUp /> : <HiOutlineChevronDown />}
                    </button>
                  </div>
                </div>
                {isExpanded && (
                  <div className="agent-timeline-card-body">
                    <div className="agent-timeline-answer-content">{event.answer}</div>
                  </div>
                )}
              </div>
            );
          }

          return (
            <div key={idx} className="agent-timeline-card generic">
              <div className="agent-timeline-card-header">
                <span className="agent-timeline-tag">{event.type || 'Event'}</span>
              </div>
              <div className="agent-timeline-card-body">
                <pre className="agent-timeline-code">{JSON.stringify(event, null, 2)}</pre>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
