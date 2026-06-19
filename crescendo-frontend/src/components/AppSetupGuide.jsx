import { useState } from 'react';
import { motion } from 'framer-motion';
import {
  HiOutlineX, HiOutlineLightningBolt, HiOutlineBookOpen,
  HiOutlineCog, HiOutlineExternalLink, HiOutlineClipboardCopy,
  HiOutlineCheckCircle, HiOutlineArrowRight, HiOutlineInformationCircle,
} from 'react-icons/hi';
import { getAppGuide, getCallbackUrl } from '../data/appGuideData';
import './AppSetupGuide.css';

const TABS = [
  { key: 'overview', label: 'Overview', icon: HiOutlineInformationCircle },
  { key: 'setup', label: 'Setup', icon: HiOutlineCog },
  { key: 'reference', label: 'Reference', icon: HiOutlineBookOpen },
];

/**
 * AppSetupGuide — premium onboarding modal for each app.
 *
 * Props:
 *   app       — catalog app object { appKey, name, description, authType, ... }
 *   onContinue — callback when user is ready to connect
 *   onClose    — close the guide
 */
export default function AppSetupGuide({ app, onContinue, onClose }) {
  const [activeTab, setActiveTab] = useState('overview');
  const [copiedSnippet, setCopiedSnippet] = useState(null);

  const guide = getAppGuide(app.appKey, app);

  if (!guide) {
    // No guide available, skip straight to connection
    onContinue?.();
    return null;
  }

  const handleCopy = (text, id) => {
    navigator.clipboard.writeText(text);
    setCopiedSnippet(id);
    setTimeout(() => setCopiedSnippet(null), 2000);
  };

  // Use theme-consistent accent — follows the app's black/white theme.
  // CSS variable resolves to dark in light mode, light in dark mode.
  const accentColor = 'var(--text-accent, #fff)';

  return (
    <motion.div
      className="asg-backdrop"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      onClick={onClose}
    >
      <motion.div
        className="asg-modal"
        initial={{ opacity: 0, scale: 0.95, y: 24 }}
        animate={{ opacity: 1, scale: 1, y: 0 }}
        exit={{ opacity: 0, scale: 0.95, y: 24 }}
        transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Accent gradient bar */}
        <div className="asg-accent-bar" />

        {/* Header */}
        <div className="asg-header">
          <div className="asg-header-left">
            <div className="asg-app-icon" style={{ overflow: 'hidden', padding: '4px' }}>
              <img 
                src={app.logoUrl || `/icons/${app.appKey}.svg`}
                alt={app.name}
                style={{
                  width: '100%', height: '100%', objectFit: 'contain',
                  filter: app.logoUrl ? 'grayscale(1) invert(1) brightness(1.5)' : 'brightness(0) invert(1)'
                }}
                onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
              />
              <span style={{ display: 'none' }}>{app.name?.charAt(0).toUpperCase()}</span>
            </div>
            <div className="asg-header-info">
              <h2 className="asg-title">{app.name}</h2>
              <span className="asg-category-badge">
                {guide.category || app.category || 'App'}
              </span>
            </div>
          </div>
          <button className="asg-close" onClick={onClose}><HiOutlineX /></button>
        </div>

        {/* Tabs */}
        <div className="asg-tabs">
          {TABS.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              className={`asg-tab ${activeTab === key ? 'active' : ''}`}
              onClick={() => setActiveTab(key)}
              style={activeTab === key ? {} : {}}
            >
              <Icon /> {label}
            </button>
          ))}
        </div>

        {/* Tab Content */}
        <div className="asg-body">

          {/* ═══ OVERVIEW TAB ═══ */}
          {activeTab === 'overview' && (
            <div className="asg-overview">
              <div className="asg-desc-card">
                <h3>What does {app.name} do?</h3>
                <p>{guide.description}</p>
              </div>

              <div className="asg-returns-card">
                <h4><HiOutlineLightningBolt /> What it returns</h4>
                <p>{guide.returns}</p>
              </div>

              {guide.examples && guide.examples.length > 0 && (
                <div className="asg-examples">
                  <h4>Example Workflows</h4>
                  <ul>
                    {guide.examples.map((ex, i) => (
                      <li key={i}>
                        <span className="asg-example-num">{i + 1}</span>
                        <span>{ex}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {guide.authType === 'none' && (
                <div className="asg-no-auth-badge">
                  <HiOutlineCheckCircle />
                  <span>No setup required — this app works out of the box!</span>
                </div>
              )}
            </div>
          )}

          {/* ═══ SETUP TAB ═══ */}
          {activeTab === 'setup' && (
            <div className="asg-setup">
              {guide.authType === 'none' ? (
                <div className="asg-no-auth-badge" style={{ marginBottom: '1rem' }}>
                  <HiOutlineCheckCircle />
                  <span>No credentials needed. Skip straight to configuring your workflow step.</span>
                </div>
              ) : guide.authType === 'oauth' ? (
                <div className="asg-oauth-note">
                  <HiOutlineCheckCircle style={{ color: '#22c55e' }} />
                  <p>This app uses <strong>OAuth</strong> — just click "Connect" and sign in with your account. No manual API key setup needed!</p>
                </div>
              ) : null}

              <div className="asg-steps">
                {guide.setupSteps.map((step, idx) => {
                  // Compute dynamic callback URL for OAuth steps
                  const callbackUrl = step.dynamicCallback
                    ? getCallbackUrl(step.dynamicCallback)
                    : step.codeSnippet;

                  return (
                    <div key={idx} className="asg-step">
                      <div className="asg-step-num">
                        {idx + 1}
                      </div>
                      <div className="asg-step-content">
                        <h4>{step.title}</h4>
                        <p>{step.detail}</p>

                        {callbackUrl && (
                          <div className="asg-code-snippet">
                            <code>{callbackUrl}</code>
                            <button
                              className="asg-copy-btn"
                              onClick={() => handleCopy(callbackUrl, `step-${idx}`)}
                              title="Copy to clipboard"
                            >
                              {copiedSnippet === `step-${idx}`
                                ? <><HiOutlineCheckCircle /> Copied!</>
                                : <><HiOutlineClipboardCopy /> Copy</>
                              }
                            </button>
                          </div>
                        )}

                        {step.link && (
                          <a
                            href={step.link}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="asg-step-link"
                          >
                            <HiOutlineExternalLink /> Open {new URL(step.link).hostname}
                          </a>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* ═══ REFERENCE TAB ═══ */}
          {activeTab === 'reference' && (
            <div className="asg-reference">
              {guide.outputFields && guide.outputFields.length > 0 ? (
                <>
                  <h4>Output Fields</h4>
                  <p className="asg-ref-intro">
                    These fields are available to use in subsequent workflow steps via the <strong>⚡ Insert Data</strong> button.
                  </p>
                  <div className="asg-output-table">
                    <div className="asg-output-header">
                      <span>Field</span>
                      <span>Description</span>
                    </div>
                    {guide.outputFields.map((f, i) => (
                      <div key={i} className="asg-output-row">
                        <code className="asg-field-name">{f.name}</code>
                        <span className="asg-field-desc">{f.desc}</span>
                      </div>
                    ))}
                  </div>
                </>
              ) : (
                <div className="asg-ref-empty">
                  <p>Output fields will be shown here after you test the step. Use the <strong>⚡ Insert Data</strong> button in subsequent steps to reference this step's output.</p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="asg-footer">
          <button className="asg-skip-btn" onClick={onContinue}>
            Skip Guide
          </button>
          <button
            className="asg-continue-btn"
            onClick={onContinue}
          >
            Continue to Connect <HiOutlineArrowRight />
          </button>
        </div>
      </motion.div>
    </motion.div>
  );
}
