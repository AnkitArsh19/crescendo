/**
 * ErrorPage — Crescendo's product-native error pages.
 *
 * Visual concept: each error is shown as an "Execution Trace" —
 * the same vertical step-timeline users see in Run Detail — with
 * the failing step highlighted exactly as a real failed step looks.
 * Zero new visual vocabulary: if you've debugged a workflow, you
 * already know how to read this page.
 *
 * Additionally, the 404, 403, and Inactive pages use the "Broken Canvas" concept:
 * real canvas nodes styled with dashed / greyed borders, animated
 * in the same language as the actual workflow builder.
 *
 * Usage:
 *   <ErrorPage code={404} />
 *   <ErrorPage code="workflow-inactive" />
 *   <ErrorPage code="webhook-browser" webhookUrl={window.location.href} />
 *   <ErrorPage showSwitcher={true} />
 */

import { useState } from 'react';
import { useNavigate, Link, useParams, useSearchParams, useLocation } from 'react-router-dom';
import {
  HiOutlineLightningBolt,
  HiOutlineCog,
  HiOutlineCheckCircle,
  HiOutlineXCircle,
  HiOutlineClock,
  HiOutlineLockClosed,
  HiOutlineArrowLeft,
  HiOutlineRefresh,
  HiOutlineExclamationCircle,
  HiOutlineShieldCheck,
  HiOutlineWifi,
  HiOutlineStop,
  HiOutlineClipboard,
  HiOutlineLightningBolt as HiBolt,
  HiOutlineCode,
} from 'react-icons/hi';
import usePageMeta from '../../hooks/usePageMeta';
import DotCanvas from '../../components/DotCanvas';
import './ErrorPage.css';

/* ─── Per-error configuration ──────────────────────────────────────── */

export const ERROR_CONFIG = {
  404: {
    key: '404',
    tabLabel: '404 Not Found',
    title: '404 — Not Found',
    variant: 'broken-canvas',
    headline: 'Trigger fired. No action found.',
    sub: 'This step was moved, renamed, or never built. The edge is still searching.',
    poem: null,
    trace: [
      { status: 'success', label: 'Request received', detail: 'GET /dashboard/…' },
      { status: 'success', label: 'Route resolved',   detail: 'Matching path…' },
      { status: 'failed',  label: 'Target not found', detail: 'No handler registered for this path' },
    ],
    cta: { label: 'Back to Dashboard', href: '/dashboard' },
    ctaAlt: { label: 'My Workflows',   href: '/dashboard/workflows' },
    note: 'If you followed a link from somewhere, it may be outdated.',
  },
  401: {
    key: '401',
    tabLabel: '401 Unauthorized',
    title: '401 — Session Required',
    variant: 'execution-trace',
    headline: 'Session not found.',
    sub: 'This branch requires authentication. Log in to continue.',
    poem: 'The orchestra assembled,\nbut the conductor never arrived.\nNo baton, no beginning.',
    trace: [
      { status: 'success', label: 'Request received',   detail: 'GET …' },
      { status: 'success', label: 'Auth header checked', detail: 'Authorization: —' },
      { status: 'failed',  label: 'Session validation',  detail: 'No active session or token expired' },
    ],
    cta:    { label: 'Sign in',       href: '/login' },
    ctaAlt: { label: 'Create account', href: '/register' },
    note: null,
  },
  403: {
    key: '403',
    tabLabel: '403 Forbidden',
    title: '403 — Access Denied',
    variant: 'broken-canvas',
    headline: "This branch isn't yours to run.",
    sub: "You're authenticated, but you don't have access to this resource. Ask the owner to share it.",
    poem: null,
    trace: [
      { status: 'success', label: 'Request received', detail: 'GET …' },
      { status: 'success', label: 'Session verified',  detail: 'User authenticated ✓' },
      { status: 'failed',  label: 'Permission check',  detail: 'FORBIDDEN — insufficient scope for this resource' },
    ],
    cta:    { label: 'Back to Dashboard', href: '/dashboard' },
    ctaAlt: null,
    note: "If you think this is a mistake, contact the workflow owner.",
  },
  429: {
    key: '429',
    tabLabel: '429 Rate Limit',
    title: '429 — Rate Limited',
    variant: 'execution-trace',
    headline: "This trigger's firing faster than we can keep up.",
    sub: 'Rate limit reached. Your requests are queuing up — slow down and try again in a moment.',
    poem: 'Too many notes, too quickly.\nEven Crescendo has a tempo.',
    trace: [
      { status: 'success', label: 'Webhook received',  detail: 'POST /webhooks/…' },
      { status: 'success', label: 'Rate check',        detail: '… / 100 req/min' },
      { status: 'failed',  label: 'Rate limit exceeded', detail: '429 Too Many Requests — back off and retry' },
    ],
    cta:    { label: 'Back to Dashboard', href: '/dashboard' },
    ctaAlt: null,
    note: 'If you need higher limits, check your plan in Settings.',
  },
  500: {
    key: '500',
    tabLabel: '500 Server Error',
    title: '500 — Server Error',
    variant: 'execution-trace',
    headline: 'Step failed unexpectedly.',
    sub: "Not your fault — ours. The error has been logged and we're looking into it.",
    poem: 'A rest where there should be a note.\nThe orchestra waits.\nWe are finding the page.',
    trace: [
      { status: 'success', label: 'Request received',    detail: 'GET …' },
      { status: 'success', label: 'Handler dispatched',  detail: 'Controller → Service' },
      { status: 'failed',  label: 'Unexpected exception', detail: 'Internal server error — logged automatically' },
    ],
    cta:    { label: 'Try Again',         href: null,        action: 'reload' },
    ctaAlt: { label: 'Back to Dashboard', href: '/dashboard' },
    note: null,
  },
  503: {
    key: '503',
    tabLabel: '503 Maintenance',
    title: '503 — Brief Maintenance',
    variant: 'execution-trace',
    headline: 'Crescendo is briefly offline.',
    sub: "We're deploying an update or running maintenance. This usually takes under 2 minutes.",
    poem: 'The curtain is drawn.\nThe stage is being set.\nThe performance resumes shortly.',
    trace: [
      { status: 'success', label: 'Request received', detail: 'GET …' },
      { status: 'pending', label: 'Server startup',   detail: 'Deployment in progress…' },
      { status: 'pending', label: 'Awaiting ready',   detail: 'Service unavailable — retrying' },
    ],
    cta:    { label: 'Refresh', href: null, action: 'reload' },
    ctaAlt: null,
    note: 'If this continues beyond 5 minutes, something unexpected happened.',
  },
  'workflow-inactive': {
    key: 'workflow-inactive',
    tabLabel: 'Workflow Inactive',
    title: 'Workflow Inactive',
    variant: 'broken-canvas',
    headline: "This workflow has been switched off.",
    sub: "The trigger node is paused. The workflow's owner has deactivated or deleted it.",
    poem: null,
    trace: [
      { status: 'success', label: 'Share link resolved', detail: 'Workflow found in catalog' },
      { status: 'success', label: 'Permission check',    detail: 'Public share link ✓' },
      { status: 'skipped', label: 'Trigger',             detail: 'INACTIVE — workflow is paused or deleted' },
    ],
    cta:    { label: 'My Workflows', href: '/dashboard/workflows' },
    ctaAlt: null,
    note: 'If this is your workflow, activate it from the canvas.',
  },
  'webhook-browser': {
    key: 'webhook-browser',
    tabLabel: 'Webhook Endpoint',
    title: 'Webhook Endpoint',
    variant: 'webhook',
    headline: 'Nice. You found the webhook endpoint.',
    sub: null,
    poem: null,
    trace: null,
    cta:    { label: 'Open Dashboard', href: '/dashboard' },
    ctaAlt: null,
    note: null,
  },
};

export function resolveErrorCode(raw) {
  if (raw === undefined || raw === null || raw === '') return 404;
  const str = String(raw).toLowerCase().trim();

  if (ERROR_CONFIG[str]) return str;
  const num = parseInt(str, 10);
  if (ERROR_CONFIG[num]) return num;

  // Semantic Aliases
  if (['unauthorized', 'unauthenticated', 'session-expired', 'login'].includes(str)) return 401;
  if (['forbidden', 'access-denied', 'denied', 'no-access'].includes(str)) return 403;
  if (['not-found', 'missing', 'broken'].includes(str)) return 404;
  if (['rate-limit', 'rate-limited', 'ratelimit', 'too-many-requests', 'throttle'].includes(str)) return 429;
  if (['server-error', 'internal-error', 'error', 'crash'].includes(str)) return 500;
  if (['maintenance', 'offline', 'service-unavailable', 'deploy', 'deploying'].includes(str)) return 503;
  if (['inactive', 'workflow-inactive', 'workflow-paused', 'paused', 'disabled'].includes(str)) return 'workflow-inactive';
  if (['webhook', 'webhooks', 'webhook-browser', 'easter-egg', 'terminal'].includes(str)) return 'webhook-browser';

  return 404;
}

/* ─── Status dot / icon helpers ─────────────────────────────────────── */

function StepIcon({ status }) {
  const map = {
    success: <HiOutlineCheckCircle className="ep-trace-icon ep-trace-icon--success" />,
    failed:  <HiOutlineXCircle     className="ep-trace-icon ep-trace-icon--failed"  />,
    pending: <HiOutlineClock        className="ep-trace-icon ep-trace-icon--pending" />,
    skipped: <HiOutlineStop         className="ep-trace-icon ep-trace-icon--skipped" />,
  };
  return map[status] || map.pending;
}

/* ─── Broken Canvas visual — vertical stacked layout ──────────────────── */

function BrokenCanvas({ code }) {
  const isLocked       = code === 403;
  const isInactive     = code === 'workflow-inactive';
  const isMissing      = code === 404;

  return (
    <div className="ep-canvas" aria-hidden="true">
      {/* Trigger node — always present and "fired" */}
      <div className="ep-node ep-node--trigger ep-node--fired">
        <div className="ep-node__badge ep-node__badge--1">1</div>
        <div className="ep-node__icon ep-node__icon--trigger">
          <HiOutlineLightningBolt />
        </div>
        <div className="ep-node__text">
          <div className="ep-node__type">Trigger</div>
          <div className="ep-node__title">
            {isInactive ? 'Workflow Paused' : 'Webhook / Schedule'}
          </div>
        </div>
        <div className="ep-node__status ep-node__status--ok" />
      </div>

      {/* Vertical connector */}
      <div className={`ep-vconnector ${isMissing || isInactive ? 'ep-vconnector--dashed' : 'ep-vconnector--solid'}`}>
        <span className="ep-vconnector__dot" />
        <span className="ep-vconnector__dot" />
        <span className="ep-vconnector__dot" />
      </div>

      {/* Action node — broken / missing / locked depending on error */}
      <div
        className={[
          'ep-node ep-node--action',
          isMissing   && 'ep-node--missing',
          isLocked    && 'ep-node--locked',
          isInactive  && 'ep-node--inactive',
        ]
          .filter(Boolean)
          .join(' ')}
      >
        <div className="ep-node__badge ep-node__badge--2">2</div>
        <div className="ep-node__icon">
          {isLocked ? <HiOutlineLockClosed /> : <HiOutlineCog />}
        </div>
        <div className="ep-node__text">
          <div className="ep-node__type">Action</div>
          <div className="ep-node__title">
            {isMissing  && 'Step not found'}
            {isLocked   && 'Access denied'}
            {isInactive && 'No output'}
          </div>
        </div>
        {isMissing && <div className="ep-node__status ep-node__status--err" />}
        {isLocked  && <div className="ep-node__status ep-node__status--lock" />}
        {isMissing && <div className="ep-node__pulse" />}
        {isLocked  && <div className="ep-node__lock-overlay"><HiOutlineLockClosed /></div>}
      </div>
    </div>
  );
}

/* ─── Execution Trace visual ─────────────────────────────────────────── */

function ExecutionTrace({ trace }) {
  return (
    <div className="ep-trace" aria-hidden="true">
      {trace.map((step, i) => (
        <div key={i} className={`ep-trace-step ep-trace-step--${step.status}`}>
          {/* Vertical connector line */}
          {i < trace.length - 1 && <div className="ep-trace-line" />}

          <StepIcon status={step.status} />
          <div className="ep-trace-content">
            <div className="ep-trace-label">{step.label}</div>
            <div className="ep-trace-detail">{step.detail}</div>
          </div>
          {step.status === 'failed' && (
            <div className="ep-trace-badge">FAILED</div>
          )}
          {step.status === 'pending' && (
            <div className="ep-trace-badge ep-trace-badge--pending">…</div>
          )}
          {step.status === 'skipped' && (
            <div className="ep-trace-badge ep-trace-badge--skipped">SKIP</div>
          )}
        </div>
      ))}
    </div>
  );
}

/* ─── Webhook Easter Egg ─────────────────────────────────────────────── */

function WebhookEasterEgg({ webhookUrl }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(webhookUrl || window.location.href).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    });
  };

  return (
    <div className="ep-webhook">
      <div className="ep-webhook__icon">
        <HiBolt />
      </div>
      <h1 className="ep-webhook__headline">
        This is a webhook endpoint.
      </h1>
      <p className="ep-webhook__sub">
        Webhooks speak{' '}
        <code>POST</code>, not browsers. You&apos;re getting a raw&nbsp;
        <code>200 OK</code> back right now because GET to this endpoint isn&apos;t wrong —
        it&apos;s just not the method Crescendo listens for.
      </p>

      <div className="ep-webhook__trace" aria-hidden="true">
        <div className="ep-webhook__step ep-webhook__step--ok">
          <HiOutlineCheckCircle />
          <span>GET request received</span>
        </div>
        <div className="ep-webhook__step ep-webhook__step--ok">
          <HiOutlineShieldCheck />
          <span>Endpoint authenticated</span>
        </div>
        <div className="ep-webhook__step ep-webhook__step--warn">
          <HiOutlineWifi />
          <span>Waiting for POST payload — this tab is not a workflow run</span>
        </div>
      </div>

      <div className="ep-webhook__url-row">
        <code className="ep-webhook__url">{webhookUrl || window.location.href}</code>
        <button
          className="ep-webhook__copy"
          onClick={handleCopy}
          title="Copy webhook URL"
        >
          {copied ? <HiOutlineCheckCircle /> : <HiOutlineClipboard />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>

      <p className="ep-webhook__tip">
        Paste this URL into your trigger source (e.g. a curl command, GitHub action, or external service)
        and send a POST with a JSON body. The workflow will fire on the next event.
      </p>
    </div>
  );
}

/* ─── Error number display ───────────────────────────────────────────── */

function ErrorCode({ code }) {
  if (typeof code === 'number') {
    return <div className="ep-code">{code}</div>;
  }
  const icons = {
    'workflow-inactive': <HiOutlineStop className="ep-code-icon" />,
    'webhook-browser':   <HiBolt className="ep-code-icon" />,
  };
  return icons[code] ? (
    <div className="ep-code ep-code--icon">{icons[code]}</div>
  ) : null;
}

/* ─── Interactive Switcher / Preview Bar ─────────────────────────────── */

function ErrorSwitcher({ currentCode }) {
  const navigate = useNavigate();
  const errorKeys = Object.keys(ERROR_CONFIG);

  return (
    <div className="ep-switcher-container">
      <div className="ep-switcher-bar">
        <div className="ep-switcher-title">
          <HiOutlineCode /> Preview Mode
        </div>
        <div className="ep-switcher-tabs">
          {errorKeys.map((key) => {
            const item = ERROR_CONFIG[key];
            const isActive = String(currentCode) === String(key);
            return (
              <button
                key={key}
                type="button"
                className={`ep-switcher-tab ${isActive ? 'active' : ''}`}
                onClick={() => navigate(`/errors/${key}`)}
              >
                {item.tabLabel}
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}

/* ─── Main exported component ────────────────────────────────────────── */

export default function ErrorPage({ code, webhookUrl, showSwitcher = false }) {
  const navigate = useNavigate();
  const location = useLocation();
  const params = useParams();
  const [searchParams] = useSearchParams();

  // Resolve code from prop, router params, or query string
  const rawCode = code ?? params.code ?? searchParams.get('code');
  const resolvedCode = resolveErrorCode(rawCode);
  const cfg = ERROR_CONFIG[resolvedCode] || ERROR_CONFIG[404];

  // Check if switcher should be active (e.g. visited /errors, /error, or ?preview=1)
  const isPreviewMode =
    showSwitcher ||
    location.pathname.startsWith('/errors') ||
    location.pathname.startsWith('/error') ||
    searchParams.get('preview') === 'true' ||
    searchParams.get('preview') === '1';

  usePageMeta(
    `${cfg.title || 'Error'} — Crescendo`,
    cfg.sub || cfg.headline || 'Crescendo Workflow Automation Error State'
  );

  const handleCta = (cta) => {
    if (cta.action === 'reload') {
      window.location.reload();
    } else if (cta.href) {
      navigate(cta.href);
    }
  };

  // Webhook gets its own full-screen layout
  if (resolvedCode === 'webhook-browser' || cfg.variant === 'webhook') {
    return (
      <div className="ep-root">
        <DotCanvas />
        <div className="ep-root__content">
          <WebhookEasterEgg webhookUrl={webhookUrl} />
          <div className="ep-webhook__actions">
            <Link to="/dashboard" className="ep-btn ep-btn--primary">
              Open Dashboard
            </Link>
          </div>
        </div>
        {isPreviewMode && <ErrorSwitcher currentCode={resolvedCode} />}
      </div>
    );
  }

  return (
    <div className="ep-root">
      <DotCanvas />
      <div className="ep-card">
        {/* Left: visual panel with watermark code behind it */}
        <div className="ep-visual">
          {/* Watermark error code — large, behind the canvas */}
          <div className="ep-code-watermark" aria-hidden="true">
            {typeof resolvedCode === 'number' ? resolvedCode : (
              resolvedCode === 'workflow-inactive' ? '!' :
              resolvedCode === 'webhook-browser' ? '#' : '?'
            )}
          </div>
          {cfg.variant === 'broken-canvas' && <BrokenCanvas code={resolvedCode} />}
          {cfg.variant === 'execution-trace' && cfg.trace && (
            <ExecutionTrace trace={cfg.trace} />
          )}
        </div>

        {/* Right: copy */}
        <div className="ep-copy">
          <div className="ep-label">
            {typeof resolvedCode === 'number'
              ? `HTTP ${resolvedCode}`
              : resolvedCode === 'workflow-inactive'
              ? 'Workflow Inactive'
              : resolvedCode}
          </div>

          <h1 className="ep-headline">{cfg.headline}</h1>
          {cfg.sub && <p className="ep-sub">{cfg.sub}</p>}

          {cfg.poem && (
            <pre className="ep-poem font-serif">{cfg.poem}</pre>
          )}

          {/* Execution trace (shown on right side for broken-canvas variant) */}
          {cfg.variant === 'broken-canvas' && cfg.trace && (
            <ExecutionTrace trace={cfg.trace} />
          )}

          {cfg.note && (
            <p className="ep-note">
              <HiOutlineExclamationCircle className="ep-note-icon" />
              {cfg.note}
            </p>
          )}

          <div className="ep-actions">
            {cfg.cta && cfg.cta.action === 'reload' && (
              <button
                className="ep-btn ep-btn--primary"
                onClick={() => handleCta(cfg.cta)}
              >
                <HiOutlineRefresh />
                {cfg.cta.label}
              </button>
            )}
            {cfg.cta && cfg.cta.action !== 'reload' && cfg.cta.href && (
              <Link to={cfg.cta.href} className="ep-btn ep-btn--primary">
                <HiOutlineArrowLeft />
                {cfg.cta.label}
              </Link>
            )}
            {cfg.ctaAlt && cfg.ctaAlt.href && (
              <Link to={cfg.ctaAlt.href} className="ep-btn ep-btn--ghost">
                {cfg.ctaAlt.label}
              </Link>
            )}
          </div>
        </div>
      </div>

      {isPreviewMode && <ErrorSwitcher currentCode={resolvedCode} />}
    </div>
  );
}
