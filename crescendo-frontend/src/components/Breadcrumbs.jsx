import { Link, useLocation } from 'react-router-dom';
import { HiChevronRight } from 'react-icons/hi';
import './Breadcrumbs.css';

/** Map raw path segments to human-readable labels. */
const LABELS = {
  dashboard: 'Dashboard',
  workflows: 'Workflows',
  history: 'History',
  connections: 'Connections',
  settings: 'Settings',
  email: 'Email Service',
  admin: 'Admin',
  new: 'New Workflow',
  profile: 'Profile',
  security: 'Security',
  accounts: 'Connected Accounts',
  'oauth-apps': 'OAuth Apps',
  'developer-api': 'Developer API',
  domains: 'Domains',
  templates: 'Templates',
  logs: 'Email Logs',
  contacts: 'Contacts',
  broadcasts: 'Broadcasts',
  analytics: 'Analytics',
  suppressions: 'Suppressions',
};

/** Segments that look like IDs (UUIDs or long hex strings) — shown as truncated monospace. */
function isId(segment) {
  return /^[0-9a-f-]{20,}$/i.test(segment);
}

function labelFor(segment) {
  if (LABELS[segment]) return LABELS[segment];
  if (isId(segment)) return segment.substring(0, 8) + '…';
  // Capitalise kebab-case words: "my-workflow" → "My Workflow"
  return segment
    .split('-')
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(' ');
}

export default function Breadcrumbs({ className = '' }) {
  const { pathname } = useLocation();

  // Only show inside /dashboard/** and /settings/** paths
  const shouldShow =
    pathname.startsWith('/dashboard') || pathname.startsWith('/settings');
  if (!shouldShow) return null;

  const segments = pathname.split('/').filter(Boolean);

  // Build crumbs: each crumb has a label + the accumulated href
  const crumbs = segments.map((seg, idx) => ({
    label: labelFor(seg),
    href: '/' + segments.slice(0, idx + 1).join('/'),
    isId: isId(seg),
  }));

  // Only show breadcrumbs when there are 2+ segments (i.e. not just "/dashboard")
  if (crumbs.length < 2) return null;

  return (
    <nav className={`breadcrumbs ${className}`} aria-label="Breadcrumb">
      <ol className="breadcrumbs__list">
        {crumbs.map((crumb, idx) => {
          const isLast = idx === crumbs.length - 1;
          return (
            <li key={crumb.href} className="breadcrumbs__item">
              {idx > 0 && (
                <HiChevronRight className="breadcrumbs__sep" aria-hidden="true" />
              )}
              {isLast ? (
                <span
                  className={`breadcrumbs__label breadcrumbs__label--current ${crumb.isId ? 'breadcrumbs__label--id' : ''}`}
                  aria-current="page"
                >
                  {crumb.label}
                </span>
              ) : (
                <Link
                  to={crumb.href}
                  className={`breadcrumbs__link ${crumb.isId ? 'breadcrumbs__label--id' : ''}`}
                >
                  {crumb.label}
                </Link>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
