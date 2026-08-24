/* eslint-disable no-unused-vars */
import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { resourceApi } from '../../api/workflowApi';
import { webhookApi } from '../../api/webhookApi';
import { connectionsApi } from '../../api/connectionsApi';
import { appCatalogApi } from '../../api/appCatalogApi';
import SearchableSelect from '../../components/ui/SearchableSelect';
import AnimatedCircularProgressBar from '../../components/ui/AnimatedCircularProgressBar';
import TestResultPanel from './TestResultPanel';
import useToastStore from '../../store/toastStore';
import useAuthStore from '../../store/authStore';
import { parseConfigSchema } from '../../workflow/workflowGraphSerializer';
import { HiCheck, HiPlus, HiLightningBolt, HiChevronRight, HiX, HiOutlinePencil, HiOutlineTrash, HiUpload } from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';
import ConditionRuleBuilder from './nodes/ConditionRuleBuilder';
import { DateTimePickerField } from './fields/DateTimePickerField';
import { FileOrUrlField } from './fields/FileOrUrlField';

// ─────────────────────────────────────────────────────────────────────────────
// Common output fields per app — used when we don't have real test data yet
// ─────────────────────────────────────────────────────────────────────────────

// Trigger output fields — what pollers actually return
const TRIGGER_OUTPUT_FIELDS = {
    'gmail': ['subject', 'fromEmail', 'fromName', 'snippet', 'id'],
    'microsoft-outlook': ['subject', 'fromEmail', 'fromName', 'bodyPreview', 'receivedDateTime', 'id'],
    'discord': ['content', 'author', 'channelId', 'guildId', 'timestamp', 'messageId'],
    'slack': ['text', 'user', 'channel', 'timestamp', 'threadTs'],
    'github': ['action', 'title', 'url', 'sender', 'repository'],
    'gitlab': ['action', 'title', 'url', 'author', 'project'],
    'google-calendar': ['eventId', 'summary', 'start', 'end', 'location'],
    'google-drive': ['fileId', 'fileName', 'mimeType', 'webViewLink'],
    'google-sheets': ['values', 'range', 'spreadsheetId'],
    'google-forms': ['responseId', 'answers', 'respondentEmail'],
    'google-slides': ['presentationId', 'title', 'slideCount'],
    'crescendo-webhook': ['body', 'headers', 'method', 'url'],
    'crescendomail': ['emailId', 'event', 'recipient', 'from', 'subject', 'timestamp', 'contactId', 'domainName', 'status', 'ip', 'userAgent', 'url'],
    'rss': ['title', 'link', 'description', 'pubDate'],
    'spotify': ['trackName', 'artistName', 'albumName', 'addedAt'],
    'linkedin': ['postId', 'author', 'content', 'timestamp'],
    'twitter': ['tweetId', 'text', 'author', 'createdAt'],
    'microsoft-teams': ['messageId', 'content', 'from', 'channelId'],
    'microsoft-excel': ['rowIndex', 'values', 'worksheetName'],
    'figma': ['fileKey', 'name', 'lastModified', 'version'],
    'strava': ['activityId', 'name', 'type', 'distance', 'movingTime'],
    'airtable': ['recordId', 'fields', 'tableName'],
    'notion': ['pageId', 'title', 'url'],
    'linear': ['issueId', 'title', 'state', 'assignee'],
    'toggl': ['entryId', 'description', 'duration', 'projectName'],
    'google-tasks': ['taskId', 'title', 'status', 'due'],
    '__default__': ['data', 'id', 'status', 'message'],
};

// ─────────────────────────────────────────────────────────────────────────────
// Webhook Setup Guide — per-app instructions shown to the user in the trigger panel
// ─────────────────────────────────────────────────────────────────────────────
const WEBHOOK_SETUP_GUIDE = {
    // ── GitHub ────────────────────────────────────────────────────────────────
    github: {
        label: 'GitHub',
        intro: 'Paste the Crescendo webhook URL into your GitHub repository webhook settings.',
        settingsUrl: (cfg) => {
            if (!cfg?.repo) return 'https://github.com';
            const repoPath = cfg.repo.includes('/') ? cfg.repo : `${cfg.owner ? cfg.owner + '/' : ''}${cfg.repo}`;
            return `https://github.com/${repoPath}/settings/hooks/new`;
        },
        settingsLabel: 'Open GitHub Webhook Settings',
        steps: [
            'Open your GitHub repository → Settings → Webhooks → Add webhook.',
            'Paste the Crescendo URL in the "Payload URL" field.',
            'Set Content type to application/json.',
            'Choose events: "Just the push event" (or select individual events).',
            'Make sure "Active" is checked, then click Add webhook.',
        ],
        note: 'GitHub sends a ping event right away. You should see ✓ in GitHub settings once connected.',
    },

    // ── GitLab ────────────────────────────────────────────────────────────────
    gitlab: {
        label: 'GitLab',
        intro: 'Register the Crescendo URL as a GitLab project webhook.',
        settingsUrl: (cfg) => {
            const path = cfg?.projectId || cfg?.repo;
            return path ? `https://gitlab.com/${path}/-/hooks` : 'https://gitlab.com';
        },
        settingsLabel: 'Open GitLab Webhook Settings',
        steps: [
            'Open your GitLab project → Settings → Webhooks → Add new webhook.',
            'Paste the Crescendo URL in the "URL" field.',
            'Select the events you want (e.g. Push events, Merge request events).',
            'Leave SSL verification enabled (recommended), then click Add webhook.',
        ],
        note: 'Use "Test" in GitLab to send a sample payload and verify the connection.',
    },

    // ── Slack ─────────────────────────────────────────────────────────────────
    slack: {
        label: 'Slack',
        intro: 'Configure your Slack App to forward events to Crescendo via Event Subscriptions.',
        settingsUrl: () => 'https://api.slack.com/apps',
        settingsLabel: 'Open Slack API Dashboard',
        steps: [
            'Go to api.slack.com/apps → select your Slack app (or create one).',
            'In the left sidebar click Event Subscriptions → toggle Enable Events ON.',
            'Paste the Crescendo URL in the "Request URL" field.',
            'Wait for Slack to show ✓ Verified (Crescendo responds to the challenge automatically).',
            'Scroll down → Subscribe to bot events → add events like message.channels, app_mention.',
            'Click Save Changes, then reinstall the app to your workspace.',
        ],
        note: 'Your Slack app must have the correct OAuth scopes for each event (e.g. channels:history for messages).',
    },

    // ── Discord ───────────────────────────────────────────────────────────────
    discord: {
        label: 'Discord',
        intro: 'Create an Incoming Webhook in your Discord server channel settings.',
        settingsUrl: () => 'https://discord.com/channels/@me',
        settingsLabel: 'Open Discord',
        steps: [
            'Open your Discord server → right-click the channel → Edit Channel.',
            'Go to the Integrations tab → Webhooks → New Webhook.',
            'Give it a name and optionally set an avatar.',
            'Click "Copy Webhook URL" — this is what external services POST to.',
            'Alternatively, paste the Crescendo URL into any service that should push events to Discord.',
        ],
        note: 'Discord webhooks are outbound (Crescendo posts TO Discord). For inbound triggers from Discord to Crescendo, a Bot with Event Gateway is needed instead.',
    },

    // ── Telegram ──────────────────────────────────────────────────────────────
    telegram: {
        label: 'Telegram',
        intro: 'Connect with @crescendo_app_bot using 1-click invite links with pre-applied permissions, or register the Crescendo webhook.',
        settingsUrl: () => 'https://t.me/crescendo_app_bot',
        settingsLabel: 'Open @crescendo_app_bot',
        steps: [
            '1-Click Start (Personal Alerts): Open https://t.me/crescendo_app_bot and click START to authorize alerts.',
            '1-Click Add to Group: Open https://t.me/crescendo_app_bot?startgroup=true to add the bot to any group.',
            '1-Click Add to Channel (as Admin): Open https://t.me/crescendo_app_bot?startchannel&admin=post_messages+edit_messages+delete_messages+pin_messages to invite as admin with permissions pre-selected.',
            'Inbound Webhook (optional): Register webhook with curl -X POST "https://api.telegram.org/bot<TOKEN>/setWebhook" -d "url=<CRESCENDO_URL>"',
        ],
        codeSnippet: (url) => `curl -X POST "https://api.telegram.org/bot<YOUR_TOKEN>/setWebhook" \\\n  -d "url=${url}"`,
        note: '💡 For groups: If your bot needs to read all messages without being tagged, open @BotFather → /setprivacy → select @crescendo_app_bot → choose "Disable".',
    },

    // ── Typeform ──────────────────────────────────────────────────────────────
    typeform: {
        label: 'Typeform',
        intro: "Add the Crescendo URL as a Typeform webhook from your form's Connect tab.",
        settingsUrl: () => 'https://admin.typeform.com',
        settingsLabel: 'Open Typeform Dashboard',
        steps: [
            'Open your Typeform and click the Connect tab at the top.',
            'Select Webhooks from the integrations list.',
            'Click Add a webhook.',
            'Paste the Crescendo URL in the destination URL field.',
            'Click Save webhook. Use "Send test request" to verify.',
        ],
        note: 'Typeform requires HTTPS endpoints. You can optionally add a secret for signature verification.',
    },

    // ── Strava ────────────────────────────────────────────────────────────────
    strava: {
        label: 'Strava',
        intro: 'Strava webhooks require an API subscription — you can use curl or Postman.',
        settingsUrl: () => 'https://www.strava.com/settings/api',
        settingsLabel: 'Open Strava API Settings',
        steps: [
            'Note your Strava Client ID and Client Secret from strava.com/settings/api.',
            'Run the following curl command (Strava will verify your endpoint immediately):',
            'curl -X POST https://www.strava.com/api/v3/push_subscriptions \\\n  -F client_id=YOUR_CLIENT_ID \\\n  -F client_secret=YOUR_CLIENT_SECRET \\\n  -F callback_url=<CRESCENDO_URL> \\\n  -F verify_token=crescendo_verify',
            'Crescendo automatically responds to the hub.challenge verification request.',
            'You will receive a subscription_id in the response confirming success.',
        ],
        codeSnippet: (url) => `curl -X POST https://www.strava.com/api/v3/push_subscriptions \\\n  -F client_id=YOUR_CLIENT_ID \\\n  -F client_secret=YOUR_CLIENT_SECRET \\\n  -F callback_url=${url} \\\n  -F verify_token=crescendo_verify`,
        note: 'Each Strava API app can have only one active webhook subscription at a time.',
    },

    // ── Figma ─────────────────────────────────────────────────────────────────
    figma: {
        label: 'Figma',
        intro: 'Figma webhooks are created via API — there is no UI for this in Figma.',
        settingsUrl: () => 'https://www.figma.com/developers/api#webhooks-v2',
        settingsLabel: 'Figma Webhooks API Docs',
        steps: [
            'Generate a Personal Access Token in Figma account settings (requires webhooks:write scope).',
            'Find your Team ID from the Figma URL when viewing your team page.',
            'Send this POST request (replace values):',
            'curl -X POST https://api.figma.com/v2/webhooks \\\n  -H "X-Figma-Token: YOUR_PAT" \\\n  -H "Content-Type: application/json" \\\n  -d \'{"event_type":"FILE_UPDATE","team_id":"TEAM_ID","endpoint":"<URL>","passcode":"any_secret","status":"ACTIVE"}\'',
            'Figma will send a PING event immediately to verify your endpoint.',
        ],
        codeSnippet: (url) => `curl -X POST https://api.figma.com/v2/webhooks \\\n  -H "X-Figma-Token: YOUR_PAT" \\\n  -H "Content-Type: application/json" \\\n  -d '{"event_type":"FILE_UPDATE","team_id":"TEAM_ID","endpoint":"${url}","passcode":"my_secret","status":"ACTIVE"}'`,
        note: 'Figma webhooks require a Professional, Organization, or Enterprise plan.',
    },

    // ── Calendly ──────────────────────────────────────────────────────────────
    calendly: {
        label: 'Calendly',
        intro: 'Calendly webhooks are registered via the Calendly API (not the dashboard UI).',
        settingsUrl: () => 'https://calendly.com/integrations/api_webhooks',
        settingsLabel: 'Open Calendly API & Webhooks',
        steps: [
            'Go to Calendly → Integrations & Apps → API & Webhooks → get your Personal Access Token.',
            'Create the subscription with this curl (replace YOUR_TOKEN and your org URI):',
            'curl -X POST https://api.calendly.com/webhook_subscriptions \\\n  -H "Authorization: Bearer YOUR_TOKEN" \\\n  -H "Content-Type: application/json" \\\n  -d \'{"url":"<URL>","events":["invitee.created","invitee.canceled"],"organization":"YOUR_ORG_URI","scope":"organization"}\'',
            'Get your organization URI from: GET https://api.calendly.com/users/me',
        ],
        codeSnippet: (url) => `curl -X POST https://api.calendly.com/webhook_subscriptions \\\n  -H "Authorization: Bearer YOUR_TOKEN" \\\n  -H "Content-Type: application/json" \\\n  -d '{"url":"${url}","events":["invitee.created","invitee.canceled"],"organization":"https://api.calendly.com/organizations/YOUR_ID","scope":"organization"}'`,
        note: 'Calendly webhooks require a Standard plan or higher.',
    },

    // ── Cal.com ───────────────────────────────────────────────────────────────
    calcom: {
        label: 'Cal.com',
        intro: 'Add the Crescendo URL from your Cal.com developer settings.',
        settingsUrl: () => 'https://app.cal.com/settings/developer/webhooks',
        settingsLabel: 'Open Cal.com Webhook Settings',
        steps: [
            'Go to app.cal.com → Settings → Developer → Webhooks.',
            'Click "+ New" to create a new webhook subscription.',
            'Paste the Crescendo URL in the "Subscriber URL" field.',
            'Select the event triggers you want (e.g. Booking Created, Booking Cancelled).',
            'Toggle Enable Webhook ON and click Create Webhook.',
        ],
        note: 'Cal.com requires HTTPS endpoints. HTTP and localhost URLs are blocked.',
    },

    // ── HubSpot ───────────────────────────────────────────────────────────────
    hubspot: {
        label: 'HubSpot',
        intro: 'Configure a webhook target inside your HubSpot Private App settings.',
        settingsUrl: () => 'https://app.hubspot.com/private-apps',
        settingsLabel: 'Open HubSpot Private Apps',
        steps: [
            'Go to HubSpot → Settings (gear icon) → Integrations → Private Apps.',
            'Click your app name, then go to the Webhooks tab.',
            'Click Edit webhooks → paste the Crescendo URL in the "Target URL" field.',
            'Click Create subscription → choose the object and event type (e.g. Contact Created).',
            'Save. HubSpot will start sending events to Crescendo immediately.',
        ],
        note: 'HubSpot webhook subscriptions must be managed in the UI — they cannot be created via API.',
    },

    // ── Instagram ─────────────────────────────────────────────────────────────
    instagram: {
        label: 'Instagram',
        intro: 'Instagram webhooks are configured via the Meta for Developers dashboard.',
        settingsUrl: () => 'https://developers.facebook.com/apps/',
        settingsLabel: 'Open Meta for Developers',
        steps: [
            'Go to developers.facebook.com → Your App → Add Product → Webhooks.',
            'Select Instagram in the webhook object dropdown.',
            'In "Callback URL" paste the Crescendo URL.',
            'Enter a Verify Token (any string you choose — Crescendo will echo it back).',
            'Click Verify and Save, then subscribe to the fields you need.',
        ],
        note: 'Requires a Meta App with Instagram Graph API product added and proper permissions.',
    },

    // ── LinkedIn ──────────────────────────────────────────────────────────────
    linkedin: {
        label: 'LinkedIn',
        intro: 'LinkedIn webhooks require Partner API access and are limited to select programs.',
        settingsUrl: () => 'https://developer.linkedin.com/',
        settingsLabel: 'LinkedIn Developer Portal',
        steps: [
            'LinkedIn real-time webhooks are only available to approved Marketing Developer Platform (MDP) partners.',
            'If you have access, go to developer.linkedin.com → Your App → Products → Webhooks.',
            'Register the Crescendo URL as the endpoint and subscribe to available events.',
        ],
        note: '⚠️ Real-time webhook triggers for LinkedIn are not available on the free/standard API tier. Polling is used instead.',
    },

    // ── Twitter / X ───────────────────────────────────────────────────────────
    twitter: {
        label: 'Twitter / X',
        intro: 'Twitter real-time webhooks (Account Activity API) require an Enterprise plan.',
        settingsUrl: () => 'https://developer.twitter.com/en/portal/dashboard',
        settingsLabel: 'Open X Developer Portal',
        steps: [
            'Twitter/X real-time webhooks require the Account Activity API (Enterprise tier).',
            'If you have access, go to developer.twitter.com → Your App → Webhooks.',
            'Register the Crescendo URL and subscribe to account activities.',
        ],
        note: '⚠️ Real-time webhook triggers for Twitter/X require an Enterprise plan. Crescendo uses polling on free/standard tiers.',
    },
};

// ─────────────────────────────────────────────────────────────────────────────
// WebhookSetupPanel Component — renders URL + copy + instructions for trigger steps
// ─────────────────────────────────────────────────────────────────────────────
function WebhookSetupPanel({ appKey, iconUrl, configuration, webhookInfo, workflowId }) {
    const [copied, setCopied] = useState(false);
    const guide = WEBHOOK_SETUP_GUIDE[appKey];

    // Non-webhook triggers don't need manual webhook setup
    const nonWebhookApps = ['schedule', 'native-form', 'gmail', 'microsoft-outlook', 'error-handling', 'imap', 'mqtt', 'kafka', 'rabbitmq', 'rss'];
    if (nonWebhookApps.includes(appKey)) return null;

    const fullUrl = webhookInfo?.url
        ? (webhookInfo.url.startsWith('http') ? webhookInfo.url : `https://api.crescendo.run${webhookInfo.url}`)
        : (workflowId ? `https://api.crescendo.run/webhooks/${workflowId}` : '');

    const handleCopy = (text) => {
        if (!text) return;
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    const settingsTarget = guide?.settingsUrl
        ? (typeof guide.settingsUrl === 'function' ? guide.settingsUrl(configuration || {}) : guide.settingsUrl)
        : null;

    const logoSrc = iconUrl || (appKey ? `/icons/${appKey}.svg` : null);

    return (
        <div style={{
            marginTop: '12px',
            marginBottom: '16px',
            borderRadius: '8px',
            overflow: 'hidden',
            border: '1px solid var(--border-secondary)',
            background: 'var(--bg-secondary)',
        }}>
            {/* Header */}
            <div style={{
                padding: '10px 14px',
                background: 'rgba(255, 255, 255, 0.03)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid var(--border-secondary)',
            }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '9px' }}>
                    {logoSrc ? (
                        <div style={{ width: '22px', height: '22px', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0, borderRadius: '4px', overflow: 'hidden' }}>
                            <img
                                src={logoSrc}
                                alt={guide?.label || appKey || ''}
                                className="app-logo-img"
                                style={{ width: '20px', height: '20px', objectFit: 'contain' }}
                                onError={(e) => {
                                    e.target.style.display = 'none';
                                    if (e.target.nextElementSibling) {
                                        e.target.nextElementSibling.style.display = 'block';
                                    }
                                }}
                            />
                            <HiOutlineBolt style={{ display: 'none', width: '18px', height: '18px', color: 'var(--text-primary)' }} />
                        </div>
                    ) : (
                        <HiOutlineBolt style={{ width: '18px', height: '18px', color: 'var(--text-primary)', flexShrink: 0 }} />
                    )}
                    <span style={{ fontSize: '0.82rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                        {guide ? `Connect ${guide.label} Webhook` : 'Webhook Inbound Trigger'}
                    </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span style={{
                        fontSize: '0.72rem',
                        fontWeight: 600,
                        padding: '2px 8px',
                        borderRadius: '12px',
                        background: webhookInfo?.isActive ? 'rgba(34, 197, 94, 0.15)' : 'rgba(255, 255, 255, 0.08)',
                        color: webhookInfo?.isActive ? '#22c55e' : '#94a3b8',
                    }}>
                        {webhookInfo?.isActive ? '● Active' : (webhookInfo ? '○ Inactive' : '● Ready')}
                    </span>
                    {settingsTarget && (
                        <a
                            href={settingsTarget}
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                                fontSize: '0.72rem',
                                color: 'var(--text-primary)',
                                textDecoration: 'none',
                                fontWeight: 500,
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '3px',
                                background: 'var(--bg-card)',
                                border: '1px solid var(--border-secondary)',
                                padding: '2px 8px',
                                borderRadius: '4px',
                            }}
                        >
                            ↗ Settings
                        </a>
                    )}
                </div>
            </div>

            {/* Body */}
            <div style={{ padding: '12px 14px' }}>
                {guide && (
                    <p style={{ fontSize: '0.78rem', color: 'var(--text-secondary)', marginBottom: '10px', lineHeight: 1.5 }}>
                        {guide.intro}
                    </p>
                )}

                {/* URL + Copy button */}
                <div style={{ fontSize: '0.74rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '5px' }}>
                    Your Crescendo Webhook URL:
                </div>
                <div style={{ display: 'flex', gap: '6px', alignItems: 'center', marginBottom: guide ? '12px' : '0' }}>
                    <input
                        type="text"
                        readOnly
                        value={fullUrl}
                        placeholder="Save workflow to generate webhook URL"
                        style={{
                            flex: 1,
                            padding: '7px 10px',
                            fontSize: '0.73rem',
                            fontFamily: 'monospace',
                            background: 'var(--bg-card)',
                            border: '1px solid var(--border-secondary)',
                            borderRadius: '6px',
                            color: 'var(--text-primary)',
                        }}
                    />
                    <button
                        type="button"
                        disabled={!fullUrl}
                        onClick={() => handleCopy(fullUrl)}
                        style={{
                            padding: '7px 12px',
                            fontSize: '0.75rem',
                            fontWeight: 600,
                            borderRadius: '6px',
                            background: !fullUrl ? 'var(--bg-card)' : (copied ? '#22c55e' : '#fff'),
                            color: !fullUrl ? 'var(--text-secondary)' : (copied ? '#fff' : '#000'),
                            border: '1px solid var(--border-secondary)',
                            cursor: !fullUrl ? 'not-allowed' : 'pointer',
                            whiteSpace: 'nowrap',
                            transition: 'background 0.2s',
                        }}
                    >
                        {copied ? '✓ Copied!' : 'Copy URL'}
                    </button>
                </div>

                {guide && (
                    <>
                        {/* Steps */}
                        <div style={{ fontSize: '0.76rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '6px' }}>
                            Setup steps:
                        </div>
                        <ol style={{ paddingLeft: '18px', margin: '0 0 10px', display: 'flex', flexDirection: 'column', gap: '5px' }}>
                            {guide.steps.map((step, i) => (
                                <li key={i} style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', lineHeight: 1.5 }}>
                                    {step}
                                </li>
                            ))}
                        </ol>

                        {/* Code snippet (curl etc.) */}
                        {guide.codeSnippet && (
                            <div style={{ marginBottom: '10px' }}>
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '4px' }}>
                                    <span style={{ fontSize: '0.73rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                                        Run this command:
                                    </span>
                                    <button
                                        type="button"
                                        onClick={() => handleCopy(guide.codeSnippet(fullUrl))}
                                        style={{
                                            fontSize: '0.7rem',
                                            padding: '2px 8px',
                                            borderRadius: '4px',
                                            background: 'var(--bg-card)',
                                            border: '1px solid var(--border-secondary)',
                                            color: 'var(--text-primary)',
                                            cursor: 'pointer',
                                        }}
                                    >
                                        Copy Command
                                    </button>
                                </div>
                                <pre style={{
                                    margin: 0,
                                    padding: '8px 10px',
                                    borderRadius: '6px',
                                    background: 'var(--bg-secondary)',
                                    border: '1px solid var(--border-secondary)',
                                    fontSize: '0.7rem',
                                    fontFamily: 'monospace',
                                    color: 'var(--text-primary)',
                                    whiteSpace: 'pre-wrap',
                                    wordBreak: 'break-all',
                                    lineHeight: 1.6,
                                }}>
                                    {guide.codeSnippet(fullUrl)}
                                </pre>
                            </div>
                        )}

                        {/* Note */}
                        {guide.note && (
                            <div style={{
                                fontSize: '0.72rem',
                                color: guide.note.startsWith('⚠️') ? '#f59e0b' : 'var(--text-secondary)',
                                background: guide.note.startsWith('⚠️') ? 'rgba(245,158,11,0.08)' : 'transparent',
                                borderRadius: '5px',
                                padding: guide.note.startsWith('⚠️') ? '6px 8px' : '0',
                                lineHeight: 1.5,
                                marginTop: '4px',
                            }}>
                                {guide.note}
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
}

// Action output fields — what handlers actually return
const ACTION_OUTPUT_FIELDS = {
    'gmail': ['provider', 'to', 'subject', 'response'],
    'microsoft-outlook': ['provider', 'statusCode', 'sentTo', 'subject', 'response'],
    'discord': ['provider', 'channelId', 'response'],
    'slack': ['provider', 'channel', 'response'],
    'github': ['provider', 'action', 'owner', 'repo', 'response'],
    'gitlab': ['provider', 'action', 'projectId', 'response'],
    'google-calendar': ['provider', 'calendarId', 'response'],
    'google-sheets': ['provider', 'action', 'spreadsheetId', 'range', 'response'],
    'google-docs': ['provider', 'response'],
    'google-drive': ['provider', 'response'],
    'google-forms': ['provider', 'formId', 'response'],
    'google-slides': ['provider', 'presentationId', 'response'],
    'google-tasks': ['provider', 'taskId', 'title', 'response'],
    'microsoft-teams': ['provider', 'response'],
    'microsoft-excel': ['provider', 'response'],
    'openai': ['response', 'text', 'model', 'usage'],
    'gemini': ['response', 'text', 'model'],
    'http': ['response', 'statusCode', 'headers', 'body'],
    'crescendo-webhook': ['provider', 'url', 'response'],
    'crescendomail': ['emailId', 'to', 'from', 'subject', 'status', 'queued', 'total', 'broadcastId', 'domainId', 'suppressed', 'openCount', 'clickCount'],
    'crescendo-email': ['provider', 'to', 'subject', 'response'],
    'airtable': ['response', 'id', 'fields'],
    'notion': ['response', 'id', 'url', 'title'],
    'linear': ['response', 'issueId', 'title', 'state'],
    'toggl': ['response', 'entryId', 'description', 'duration'],
    'cat-facts': ['response', 'fact', 'length'],
    'giphy': ['response', 'url', 'title', 'resultCount'],
    'quotes': ['response', 'quote', 'author', 'category'],
    'joke-api': ['response', 'joke', 'setup', 'delivery', 'type'],
    'nasa-apod': ['response', 'title', 'url', 'explanation', 'date', 'photoCount'],
    'weather': ['response', 'city', 'temperature', 'description', 'forecastCount'],
    'linkedin': ['response', 'profile', 'id'],
    'twitter': ['response', 'tweetId', 'text'],
    'figma': ['response', 'fileKey', 'name'],
    'strava': ['response', 'activityId', 'name', 'type', 'distance'],
    'github-stats': ['response', 'username', 'repos', 'followers'],
    'leetcode': ['response', 'username', 'solved', 'query'],
    'pomodoro': ['startTime', 'endTime', 'durationMinutes', 'label', 'task'],
    'sarvam': ['response', 'translatedText', 'audios'],
    'log': ['message'],
    'job-search': ['jobs', 'totalFound', 'totalBeforeDedup', 'sources', 'query', 'location'],
    'spotify': ['response', 'tracks', 'artists', 'albums', 'playlists'],
    'telegram': ['response', 'messageId', 'chatId'],
    '__default__': ['response', 'status', 'data', 'message'],
};

function getOutputFieldsForApp(appKey, isTriggerStep) {
    if (isTriggerStep) {
        return TRIGGER_OUTPUT_FIELDS[appKey] || TRIGGER_OUTPUT_FIELDS['__default__'];
    }
    return ACTION_OUTPUT_FIELDS[appKey] || ACTION_OUTPUT_FIELDS['__default__'];
}

// ─────────────────────────────────────────────────────────────────────────────
// DynamicDropdownField — uses SearchableSelect + resourceApi
// ─────────────────────────────────────────────────────────────────────────────

function DynamicDropdownField({ field, appKey, connectionId, credentialSource, config, value, onChange }) {
    const [options, setOptions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    // Set when the AI guessed a label that doesn't match any real option ID or label
    const [aiMismatch, setAiMismatch] = useState(false);
    const prevParamsRef = useRef('');

    // In ADMIN_KEY mode, pass the sentinel string; the backend will resolve the platform bot token
    const effectiveConnectionId = (credentialSource === 'ADMIN_KEY' || (!connectionId && credentialSource !== 'PERSONAL'))
        ? 'ADMIN_KEY'
        : connectionId;

    const dependsOn = Array.isArray(field.dependsOn) ? field.dependsOn : [];

    const canFetch = appKey && effectiveConnectionId && field.resourceType
        && dependsOn.every((dep) => config[dep]);

    const fetchOptions = useCallback(async () => {
        if (!canFetch) return;
        const params = {};
        dependsOn.forEach((dep) => { params[dep] = config[dep]; });

        const paramKey = JSON.stringify(params);
        if (paramKey === prevParamsRef.current) return;
        prevParamsRef.current = paramKey;

        setLoading(true);
        setError(null);
        setAiMismatch(false);
        try {
            const data = await resourceApi.list(appKey, field.resourceType, effectiveConnectionId, params);
            const mapped = (data || []).map((o) => ({
                id: o.id,
                label: o.label || o.id,
                description: o.description && o.description !== '0 items'
                    ? `${o.description} · ID: ${o.id}`
                    : `ID: ${o.id}`,
            }));
            setOptions(mapped);

            // ── Section 4: AI guess auto-mapping ──────────────────────────────
            // If the current value exists in the options as an ID already,
            // no action needed. If it looks like a plain-text label (AI guess),
            // try to find a case-insensitive label match and upgrade it silently.
            // If no match at all, flag it in red for the user to correct manually.
            if (value && mapped.length > 0) {
                const exactIdMatch = mapped.some((o) => o.id === value);
                if (!exactIdMatch) {
                    const lv = String(value).toLowerCase();
                    const labelMatch = mapped.find(
                        (o) => o.label.toLowerCase() === lv
                             || o.id.toLowerCase() === lv
                    );
                    if (labelMatch) {
                        // Silent upgrade: replace AI-guessed text with real ID
                        onChange(labelMatch.id);
                        setAiMismatch(false);
                    } else {
                        // No match — flag for user attention
                        setAiMismatch(true);
                    }
                }
            }
            // ──────────────────────────────────────────────────────────────────
        } catch {
            setError('Failed to load options');
            setOptions([]);
        } finally {
            setLoading(false);
        }
    }, [canFetch, appKey, connectionId, field.resourceType, field.dependsOn, config, value, onChange]);

    // eslint-disable-next-line react-hooks/set-state-in-effect
    useEffect(() => { fetchOptions(); }, [fetchOptions]);

    useEffect(() => {
        if (!canFetch) {
            // eslint-disable-next-line react-hooks/set-state-in-effect
            setOptions([]);
            setAiMismatch(false);
            prevParamsRef.current = '';
        }
    }, [canFetch]);

    if (!canFetch && field.dependsOn.length > 0) {
        return (
            <SearchableSelect
                options={[]}
                value=""
                placeholder={`Select ${field.dependsOn.join(', ')} first…`}
                disabled
            />
        );
    }

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
            {aiMismatch && (
                <div className="ai-mismatch-warning" role="alert">
                    <span className="ai-mismatch-warning__icon">⚠</span>
                    <span>
                        AI picked <strong>&ldquo;{value}&rdquo;</strong> but it wasn&apos;t found.
                        Please select the correct {field.label.toLowerCase()} below.
                    </span>
                </div>
            )}
            <SearchableSelect
                options={options}
                value={aiMismatch ? '' : (value || '')}
                onChange={(v) => { setAiMismatch(false); onChange(v); }}
                placeholder={aiMismatch ? `⚠ Select ${field.label}…` : `Select ${field.label}…`}
                loading={loading}
                error={error}
                allowCustom={true}
                onRefresh={() => { prevParamsRef.current = ''; fetchOptions(); }}
                emptyMessage={`No ${field.label.toLowerCase()} found`}
                style={aiMismatch ? { borderColor: 'var(--color-danger, #ef4444)' } : undefined}
            />
            {appKey === 'discord' && (field.key === 'guildId' || field.resourceType === 'guilds') && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '3px', padding: '0 2px' }}>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>
                        Don&apos;t see your server?
                    </span>
                    <button
                        type="button"
                        style={{
                            background: 'none',
                            border: 'none',
                            color: 'var(--accent-primary, #60a5fa)',
                            fontSize: '0.73rem',
                            cursor: 'pointer',
                            padding: '0',
                            textDecoration: 'underline',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px'
                        }}
                        onClick={async () => {
                            try {
                                const { authorizationUrl } = await appCatalogApi.getOAuthUrl('discord');
                                if (authorizationUrl) {
                                    window.open(authorizationUrl, '_blank', 'width=600,height=700');
                                }
                            } catch {
                                window.open('https://discord.com/oauth2/authorize?client_id=1482384946461937777&scope=bot&permissions=534723950672', '_blank', 'width=600,height=700');
                            }
                        }}
                        title="Open Discord to invite the bot to another server"
                    >
                        + Add Bot to another server
                    </button>
                </div>
            )}
            {appKey === 'telegram' && field.resourceType === 'chats' && (
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: '6px', padding: '0 2px', flexWrap: 'wrap', gap: '6px' }}>
                    <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>
                        Can&apos;t find your chat?
                    </span>
                    <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                        <a
                            href="https://t.me/crescendo_app_bot"
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                                color: 'var(--text-secondary)',
                                fontSize: '0.73rem',
                                textDecoration: 'none',
                                fontWeight: 500,
                            }}
                        >
                            + Direct Chat
                        </a>
                        <span style={{ color: 'var(--border-primary)', fontSize: '0.7rem' }}>•</span>
                        <a
                            href="https://t.me/crescendo_app_bot?startgroup=true"
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                                color: 'var(--text-secondary)',
                                fontSize: '0.73rem',
                                textDecoration: 'none',
                                fontWeight: 500,
                            }}
                        >
                            + Group
                        </a>
                        <span style={{ color: 'var(--border-primary)', fontSize: '0.7rem' }}>•</span>
                        <a
                            href="https://t.me/crescendo_app_bot?startchannel&admin=post_messages+edit_messages+delete_messages+pin_messages"
                            target="_blank"
                            rel="noopener noreferrer"
                            style={{
                                color: 'var(--text-secondary)',
                                fontSize: '0.73rem',
                                textDecoration: 'none',
                                fontWeight: 500,
                            }}
                        >
                            + Channel
                        </a>
                    </div>
                </div>
            )}
        </div>
    );
}


// ─────────────────────────────────────────────────────────────────────────────
// VariableInsertButton — dropdown to insert {{step.N.field}} references
// ─────────────────────────────────────────────────────────────────────────────

export function VariableInsertButton({ availableVariables, onInsert }) {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const [activeTab, setActiveTab] = useState('steps'); // 'steps' | 'system'
    const ref = useRef(null);

    // Close on outside click
    useEffect(() => {
        if (!open) return;
        const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [open]);

    const systemTokens = [
        { label: 'Execution Time (Now)', token: '{{now}}', desc: 'ISO 8601 UTC timestamp at workflow runtime' },
        { label: 'Now + 2 Minutes', token: '{{now + 2m}}', desc: '2 minutes after execution begins' },
        { label: 'Now + 5 Minutes', token: '{{now + 5m}}', desc: '5 minutes after execution begins' },
        { label: 'Now + 15 Minutes', token: '{{now + 15m}}', desc: '15 minutes after execution begins' },
        { label: 'Now + 1 Hour', token: '{{now + 1h}}', desc: '1 hour after execution begins' },
        { label: 'Tomorrow (Now + 24h)', token: '{{now + 1d}}', desc: '24 hours after execution begins' },
        { label: 'Today (Date Only)', token: '{{today}}', desc: 'YYYY-MM-DD date at execution runtime' },
        { label: 'Unix Timestamp (ms)', token: '{{timestamp}}', desc: 'Current epoch in milliseconds' },
        { label: 'Unix Timestamp (sec)', token: '{{timestamp_sec}}', desc: 'Current epoch in seconds' },
    ];

    const hasVars = availableVariables && availableVariables.length > 0;

    const filteredSteps = search.trim()
        ? (availableVariables || []).map((group) => ({
            ...group,
            fields: group.fields.filter((f) => f.toLowerCase().includes(search.toLowerCase()) || group.appName.toLowerCase().includes(search.toLowerCase())),
        })).filter((g) => g.fields.length > 0)
        : (availableVariables || []);

    const filteredSystem = search.trim()
        ? systemTokens.filter(t => t.label.toLowerCase().includes(search.toLowerCase()) || t.token.toLowerCase().includes(search.toLowerCase()))
        : systemTokens;

    return (
        <div className="var-insert" ref={ref}>
            <button
                type="button"
                className="var-insert__btn"
                title="Insert dynamic data from a previous step or system variable"
                onClick={() => setOpen(!open)}
            >
                <HiLightningBolt /> <span>Insert Data</span>
            </button>
            {open && (
                <div className="var-insert__dropdown">
                    <div className="var-insert__header">
                        <div className="var-insert__tabs">
                            <button
                                type="button"
                                className={`var-insert__tab ${activeTab === 'steps' ? 'active' : ''}`}
                                onClick={() => setActiveTab('steps')}
                            >
                                Step Outputs ({availableVariables?.length || 0})
                            </button>
                            <button
                                type="button"
                                className={`var-insert__tab ${activeTab === 'system' ? 'active' : ''}`}
                                onClick={() => setActiveTab('system')}
                            >
                                Dynamic Time
                            </button>
                        </div>
                        <div className="var-insert__search">
                            <input
                                type="text"
                                placeholder={activeTab === 'steps' ? "Search step fields…" : "Search system tokens…"}
                                value={search}
                                onChange={(e) => setSearch(e.target.value)}
                                autoFocus
                            />
                        </div>
                    </div>

                    <div className="var-insert__list">
                        {activeTab === 'steps' && (
                            <>
                                {filteredSteps.map((group) => (
                                    <div key={group.stepIndex} className="var-insert__group">
                                        <div className="var-insert__group-header">
                                            <span className="var-insert__step-badge">{group.stepIndex}</span>
                                            <span className="var-insert__app-name">{group.appName}</span>
                                            <span className="var-insert__op-tag">{group.operationName}</span>
                                        </div>
                                        {group.fields.map((fieldName) => (
                                            <button
                                                key={fieldName}
                                                type="button"
                                                className="var-insert__field"
                                                onClick={() => {
                                                    onInsert(`{{steps.${group.stepIndex}.${fieldName}}}`);
                                                    setOpen(false);
                                                    setSearch('');
                                                }}
                                            >
                                                <span className="var-insert__field-name">{fieldName}</span>
                                                <span className="var-insert__field-ref">steps.{group.stepIndex}.{fieldName}</span>
                                            </button>
                                        ))}
                                    </div>
                                ))}
                                {filteredSteps.length === 0 && (
                                    <div className="var-insert__empty">
                                        {hasVars ? 'No matching fields found' : 'No previous steps available to select data from.'}
                                    </div>
                                )}
                            </>
                        )}

                        {activeTab === 'system' && (
                            <div className="var-insert__system-list">
                                {filteredSystem.map((item) => (
                                    <button
                                        key={item.token}
                                        type="button"
                                        className="var-insert__system-item"
                                        onClick={() => {
                                            onInsert(item.token);
                                            setOpen(false);
                                            setSearch('');
                                        }}
                                    >
                                        <div className="var-insert__system-title">
                                            <span>{item.label}</span>
                                            <code>{item.token}</code>
                                        </div>
                                        <div className="var-insert__system-desc">{item.desc}</div>
                                    </button>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// DynamicField — renders the correct input per field type
// ─────────────────────────────────────────────────────────────────────────────

const LOGIC_OPERATORS = [
    { value: 'equals', label: '= Equals' },
    { value: 'notEquals', label: '≠ Does not equal' },
    { value: 'contains', label: '⊃ Contains text' },
    { value: 'notContains', label: '⊅ Does not contain' },
    { value: 'startsWith', label: '^ Starts with' },
    { value: 'endsWith', label: '$ Ends with' },
    { value: 'greaterThan', label: '> Greater than' },
    { value: 'lessThan', label: '< Less than' },
    { value: 'greaterThanOrEqual', label: '≥ Greater than or equal' },
    { value: 'lessThanOrEqual', label: '≤ Less than or equal' },
    { value: 'isEmpty', label: '∅ Is empty / null' },
    { value: 'isNotEmpty', label: '✓ Is not empty' },
    { value: 'isTrue', label: '✓ Is true (Boolean)' },
    { value: 'isFalse', label: '✗ Is false (Boolean)' },
    { value: 'regex', label: '* Matches regex' }
];

function LogicRuleBuilder({ field, value, onChange, availableVariables }) {
    const isSwitch = field.key === 'rules';
    const rules = Array.isArray(value) ? value : [];
    const updateRule = (index, patch) => onChange(rules.map((rule, i) => i === index ? { ...rule, ...patch } : rule));
    const removeRule = (index) => onChange(rules.filter((_, i) => i !== index));
    const addRule = () => onChange([...rules, isSwitch
        ? { value: '', operator: 'equals', matchValue: '', outputIndex: 0 }
        : { combinator: 'AND', conditions: [{ leftValue: '', operator: 'equals', rightValue: '' }] }
    ]);

    if (!isSwitch) {
        const updateCondition = (groupIndex, conditionIndex, patch) => onChange(rules.map((group, i) => i !== groupIndex ? group : {
            ...group,
            conditions: (group.conditions || []).map((condition, ci) => ci === conditionIndex ? { ...condition, ...patch } : condition),
        }));
        return (
            <div className="logic-rule-builder">
                {rules.map((group, groupIndex) => (
                    <div className="logic-rule-group" key={groupIndex}>
                        <div className="logic-rule-group__header">
                            <select
                                className="cpb-select logic-rule-combinator"
                                value={group.combinator || 'AND'}
                                onChange={(e) => updateRule(groupIndex, { combinator: e.target.value })}
                            >
                                <option value="AND">AND (All conditions must match)</option>
                                <option value="OR">OR (Any condition matches)</option>
                            </select>
                            <button type="button" className="logic-rule-remove-group" onClick={() => removeRule(groupIndex)} aria-label="Remove condition group">
                                <HiX />
                            </button>
                        </div>
                        {(group.conditions || []).map((condition, conditionIndex) => {
                            const isUnary = ['isEmpty', 'isNotEmpty', 'isTrue', 'isFalse'].includes(condition.operator);
                            return (
                                <div className="logic-rule-row" key={conditionIndex}>
                                    <div className="logic-rule-field-wrap">
                                        <input
                                            className="cpb-input logic-rule-input"
                                            value={condition.leftValue || ''}
                                            placeholder="Select step field or type value…"
                                            onChange={(e) => updateCondition(groupIndex, conditionIndex, { leftValue: e.target.value })}
                                        />
                                        <VariableInsertButton
                                            availableVariables={availableVariables}
                                            onInsert={(tpl) => updateCondition(groupIndex, conditionIndex, { leftValue: tpl })}
                                        />
                                    </div>
                                    <select
                                        className="cpb-select logic-rule-operator"
                                        value={condition.operator || 'equals'}
                                        onChange={(e) => updateCondition(groupIndex, conditionIndex, { operator: e.target.value })}
                                    >
                                        {LOGIC_OPERATORS.map((op) => (
                                            <option value={op.value} key={op.value}>{op.label}</option>
                                        ))}
                                    </select>
                                    {!isUnary ? (
                                        <div className="logic-rule-field-wrap">
                                            <input
                                                className="cpb-input logic-rule-input"
                                                value={condition.rightValue || ''}
                                                placeholder="Compare with value…"
                                                onChange={(e) => updateCondition(groupIndex, conditionIndex, { rightValue: e.target.value })}
                                            />
                                            <VariableInsertButton
                                                availableVariables={availableVariables}
                                                onInsert={(tpl) => updateCondition(groupIndex, conditionIndex, { rightValue: tpl })}
                                            />
                                        </div>
                                    ) : (
                                        <div className="logic-rule-unary-pill">Unary check</div>
                                    )}
                                    <button
                                        type="button"
                                        className="logic-rule-remove-row"
                                        onClick={() => {
                                            const nextConds = (group.conditions || []).filter((_, ci) => ci !== conditionIndex);
                                            updateRule(groupIndex, { conditions: nextConds });
                                        }}
                                        aria-label="Remove condition"
                                    >
                                        <HiX />
                                    </button>
                                </div>
                            );
                        })}
                        <button
                            type="button"
                            className="logic-rule-add"
                            onClick={() => updateRule(groupIndex, { conditions: [...(group.conditions || []), { leftValue: '', operator: 'equals', rightValue: '' }] })}
                        >
                            <HiPlus /> Add condition
                        </button>
                    </div>
                ))}
                <button type="button" className="logic-rule-add logic-rule-add--group" onClick={addRule}>
                    <HiPlus /> Add condition group
                </button>
            </div>
        );
    }

    return (
        <div className="logic-rule-builder">
            {rules.map((rule, index) => {
                const isUnary = ['isEmpty', 'isNotEmpty', 'isTrue', 'isFalse'].includes(rule.operator);
                return (
                    <div className="logic-rule-row logic-rule-row--switch" key={index}>
                        <div className="logic-rule-field-wrap">
                            <input
                                className="cpb-input logic-rule-input"
                                value={rule.value || ''}
                                placeholder="Select field to evaluate…"
                                onChange={(e) => updateRule(index, { value: e.target.value })}
                            />
                            <VariableInsertButton
                                availableVariables={availableVariables}
                                onInsert={(tpl) => updateRule(index, { value: tpl })}
                            />
                        </div>
                        <select
                            className="cpb-select logic-rule-operator"
                            value={rule.operator || 'equals'}
                            onChange={(e) => updateRule(index, { operator: e.target.value })}
                        >
                            {LOGIC_OPERATORS.map((op) => (
                                <option value={op.value} key={op.value}>{op.label}</option>
                            ))}
                        </select>
                        {!isUnary ? (
                            <div className="logic-rule-field-wrap">
                                <input
                                    className="cpb-input logic-rule-input"
                                    value={rule.matchValue || ''}
                                    placeholder="Match value…"
                                    onChange={(e) => updateRule(index, { matchValue: e.target.value })}
                                />
                                <VariableInsertButton
                                    availableVariables={availableVariables}
                                    onInsert={(tpl) => updateRule(index, { matchValue: tpl })}
                                />
                            </div>
                        ) : (
                            <div className="logic-rule-unary-pill">Unary check</div>
                        )}
                        <div className="logic-rule-output-index">
                            <span>Output:</span>
                            <input
                                type="number"
                                min="0"
                                className="cpb-input"
                                value={rule.outputIndex ?? 0}
                                aria-label="Output index"
                                onChange={(e) => updateRule(index, { outputIndex: Number(e.target.value) })}
                            />
                        </div>
                        <button type="button" className="logic-rule-remove-row" onClick={() => removeRule(index)} aria-label="Remove rule">
                            <HiX />
                        </button>
                    </div>
                );
            })}
            <button type="button" className="logic-rule-add" onClick={addRule}>
                <HiPlus /> Add routing rule
            </button>
        </div>
    );
}

function DynamicField({ field, appKey, connectionId, credentialSource, config, value, onChange, availableVariables }) {
    const inputRef = useRef(null);

    // Insert variable template at cursor position or append
    const handleInsertVariable = (template) => {
        const el = inputRef.current;
        if (el) {
            const start = el.selectionStart ?? (value || '').length;
            const end = el.selectionEnd ?? start;
            const current = value || '';
            const newVal = current.slice(0, start) + template + current.slice(end);
            onChange(newVal);
            // Restore cursor after the inserted template
            setTimeout(() => {
                el.focus();
                const pos = start + template.length;
                el.setSelectionRange(pos, pos);
            }, 0);
        } else {
            onChange((value || '') + template);
        }
    };

    const hasVars = availableVariables && availableVariables.length > 0;

    if (appKey === 'logic' && ['conditions', 'rules'].includes(field.key)) {
        return <LogicRuleBuilder field={field} value={value} onChange={onChange} availableVariables={availableVariables} />;
    }

    switch (field.type) {
        case 'dynamic_dropdown':
            return (
                <DynamicDropdownField
                    field={field} appKey={appKey} connectionId={connectionId}
                    credentialSource={credentialSource}
                    config={config} value={value} onChange={onChange}
                />
            );

        case 'multi_select_tags': {
            const tags = Array.isArray(value) ? value : (value ? String(value).split(',').map(s => s.trim()).filter(Boolean) : []);
            const presetOptions = (field.options || []).map(o => typeof o === 'string' ? o : o.label || o.value);
            // eslint-disable-next-line react-hooks/rules-of-hooks
            const [tagInput, setTagInput] = useState('');
            // eslint-disable-next-line react-hooks/rules-of-hooks
            const [showSuggestions, setShowSuggestions] = useState(false);
            const suggestions = presetOptions.filter(o => !tags.includes(o) && o.toLowerCase().includes(tagInput.toLowerCase()));
            const addTag = (t) => { if (t && !tags.includes(t)) { onChange([...tags, t]); } setTagInput(''); setShowSuggestions(false); };
            const removeTag = (t) => onChange(tags.filter(x => x !== t));
            return (
                <div className="cpb-tags-container">
                    <div className="cpb-tags-wrap">
                        {tags.map(t => (
                            <span key={t} className="cpb-tag">{t}<button type="button" className="cpb-tag-x" onClick={() => removeTag(t)}><HiX /></button></span>
                        ))}
                        <input
                            className="cpb-tag-input"
                            value={tagInput}
                            placeholder={tags.length === 0 ? (field.placeholder || 'Type to add…') : 'Add more…'}
                            onChange={e => { setTagInput(e.target.value); setShowSuggestions(true); }}
                            onFocus={() => setShowSuggestions(true)}
                            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                            onKeyDown={e => {
                                if (e.key === 'Enter' && tagInput.trim()) { e.preventDefault(); addTag(tagInput.trim()); }
                                if (e.key === 'Backspace' && !tagInput && tags.length) removeTag(tags[tags.length - 1]);
                            }}
                        />
                    </div>
                    {showSuggestions && suggestions.length > 0 && (
                        <div className="cpb-tags-suggestions">
                            {suggestions.slice(0, 8).map(s => (
                                <button key={s} type="button" className="cpb-tags-suggestion" onMouseDown={() => addTag(s)}>{s}</button>
                            ))}
                        </div>
                    )}
                </div>
            );
        }

        case 'dropdown':
            return (
                <SearchableSelect
                    options={(field.options || []).map((opt) =>
                        typeof opt === 'string' ? { id: opt, label: opt } : { id: opt.value, label: opt.label }
                    )}
                    value={value || ''}
                    onChange={onChange}
                    placeholder={`Select ${field.label}…`}
                    searchable={field.options?.length > 6}
                />
            );

        case 'select':
            return (
                <select
                    className="cpb-input cpb-select"
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                >
                    {(field.options || []).map((opt) => {
                        const optVal = typeof opt === 'string' ? opt : (opt.value ?? '');
                        const optLabel = typeof opt === 'string' ? opt : (opt.label || opt.value || '');
                        return (
                            <option key={optVal} value={optVal}>{optLabel}</option>
                        );
                    })}
                </select>
            );

        case 'textarea':
            return (
                <div className="cpb-input-with-vars">
                    <textarea
                        ref={inputRef}
                        className="cpb-input cpb-textarea"
                        value={value || ''}
                        placeholder={field.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'boolean':
            return (
                <label className="cpb-checkbox-label">
                    <input
                        type="checkbox"
                        checked={value === true || value === 'true'}
                        onChange={(e) => onChange(e.target.checked)}
                        className="cpb-checkbox"
                    />
                    <span>{field.label}</span>
                </label>
            );

        case 'number':
            return (
                <input
                    className="cpb-input" type="number"
                    value={value ?? ''} placeholder={field.placeholder}
                    onChange={(e) => onChange(e.target.value)}
                />
            );

        case 'json':
            return (
                <div className="cpb-input-with-vars">
                    <textarea
                        ref={inputRef}
                        className="cpb-input cpb-json"
                        value={typeof value === 'object' ? JSON.stringify(value, null, 2) : (value || '')}
                        placeholder={field.placeholder || '{}'}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'password':
            return (
                <input
                    className="cpb-input" type="password"
                    value={value || ''} placeholder={field.placeholder}
                    onChange={(e) => onChange(e.target.value)}
                />
            );

        case 'array':
            return (
                <div className="cpb-input-with-vars">
                    <input
                        ref={inputRef}
                        className="cpb-input"
                        value={Array.isArray(value) ? value.join(', ') : (value || '')}
                        placeholder={field.placeholder || 'value1, value2, value3'}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'file':
        case 'file_or_url':
        case 'media':
        case 'video_upload':
            return (
                <FileOrUrlField
                    field={field}
                    value={value}
                    onChange={onChange}
                    availableVariables={availableVariables}
                />
            );

        case 'datetime':
        case 'date':
        case 'time':
            return (
                <DateTimePickerField
                    field={field}
                    value={value}
                    onChange={onChange}
                    availableVariables={availableVariables}
                />
            );

        default: // text
            if ([
                'startDate', 'endDate', 'start', 'end', 'dueDate', 'due',
                'completed', 'completedMax', 'completedMin', 'dueMax', 'dueMin',
                'updatedMin', 'timeMin', 'timeMax', 'dateTime', 'maxDateAndTime',
                'publishedAfter', 'publishedBefore', 'date', 'since', 'until',
                'before', 'after', 'createdAfter', 'createdBefore', 'startTime', 'endTime'
            ].includes(field.key)) {
                return (
                    <DateTimePickerField
                        field={field}
                        value={value}
                        onChange={onChange}
                        availableVariables={availableVariables}
                    />
                );
            }
            return (
                <div className="cpb-input-with-vars">
                    <input
                        ref={inputRef}
                        className="cpb-input" type="text"
                        value={value || ''} placeholder={field.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TABS
// ─────────────────────────────────────────────────────────────────────────────

const TABS = ['Setup', 'Configure', 'Test'];

// ─────────────────────────────────────────────────────────────────────────────
// ConfigPanelBody — 3-tab stepper
// ─────────────────────────────────────────────────────────────────────────────

export default function ConfigPanelBody({
    workflowId,
    configNode, updateNodeData, connectedAppOptions, selectedConnections,
    selectedTriggerOptions, selectedActionOptions, ensureAppDetail, appDetailsByKey,
    catalogApps, allNodes, allConnections, onOpenAppBrowser,
    onClose, onDelete, onClear, onSaveAndClose, onNavigate, nodeCount, nodeIndex,
}) {
    const { data = {} } = configNode || {};
    const isTrigger = configNode.type === 'trigger' || configNode.data?.isTrigger || configNode.data?.nodeType === 'trigger' || (!configNode.data?.actionKey && !!configNode.data?.triggerKey) || nodeIndex === 0;
    const isAgentNode = configNode.type === 'agent' || data?.appKey === 'agent';
    const [activeTab, setActiveTab] = useState(0);
    const [editingName, setEditingName] = useState(false);
    const [stepName, setStepName] = useState('');
    const [accountMenuOpen, setAccountMenuOpen] = useState(false);
    const [webhookInfo, setWebhookInfo] = useState(null);
    const [copiedWebhook, setCopiedWebhook] = useState(false);
    const accountMenuRef = useRef(null);

    const user = useAuthStore(state => state.user);
    const isAdmin = user?.role === 'ADMIN';

    // Fetch webhook endpoint details for trigger steps
    useEffect(() => {
        if (!isTrigger || !workflowId) return;
        webhookApi.list(workflowId)
            .then(webhooks => {
                if (Array.isArray(webhooks) && webhooks.length > 0) {
                    setWebhookInfo(webhooks[0]);
                }
            })
            .catch(() => {});
    }, [isTrigger, workflowId]);

    // ── Agent node auto-init ──
    // When an agent node opens without its appKey/actionKey populated, silently seed
    // the node data and default configuration so the configuration panel is ready immediately.
    useEffect(() => {
        if (!isAgentNode) return;
        const agentMeta = (catalogApps || []).find((a) => a.appKey === 'agent');
        const updates = {};
        if (!data.appKey) {
            updates.appKey = 'agent';
            updates.app = 'agent';
            updates.appName = agentMeta?.name || 'AI Agent';
            updates.iconUrl = agentMeta?.logoUrl || '/icons/agent.svg';
        }
        if (!data.actionKey) {
            updates.actionKey = 'agent:ai_agent';
            updates.action = 'agent:ai_agent';
            updates.actionName = 'AI Agent';
            updates.label = data.label && data.label !== 'Untitled Step' && data.label !== 'Select Action' ? data.label : 'AI Agent';
        }
        if (!data.configuration || Object.keys(data.configuration).length === 0) {
            updates.configuration = {
                provider: "gemini",
                model: "gemini-2.5-flash",
                systemPrompt: "You are a helpful AI assistant. Analyze the incoming data and dynamically choose the appropriate tools to accomplish the goal.",
                prompt: "{{steps.1.data}}",
                temperature: 0.7,
                maxIterations: 10,
                returnIntermediateSteps: true,
            };
        }
        if (Object.keys(updates).length > 0) {
            updateNodeData(configNode.id, updates);
        }
        ensureAppDetail('agent');
    // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [isAgentNode, configNode.id, data.appKey, data.actionKey]);

    // ── Resolve configSchema ──
    const appDetail = appDetailsByKey?.[data.appKey];
    const actionOrTriggerKey = isTrigger ? (data.triggerKey || data.actionKey) : data.actionKey;

    const configSchema = useMemo(() => {
        if (isAgentNode) {
            const currentProvider = data.configuration?.provider || 'gemini';
            let modelOptions = [
                { id: 'gemini-2.5-flash', label: 'Gemini 2.5 Flash (Recommended - Fast & Smart)' },
                { id: 'gemini-2.5-pro', label: 'Gemini 2.5 Pro (Deep Reasoning)' },
                { id: 'gemini-1.5-flash', label: 'Gemini 1.5 Flash' },
                { id: 'gemini-1.5-pro', label: 'Gemini 1.5 Pro' },
            ];
            if (currentProvider === 'openai') {
                modelOptions = [
                    { id: 'gpt-4o', label: 'GPT-4o (OpenAI)' },
                    { id: 'gpt-4o-mini', label: 'GPT-4o Mini (Fast)' },
                ];
            } else if (currentProvider === 'groq') {
                modelOptions = [
                    { id: 'llama-3.3-70b-versatile', label: 'Llama 3.3 70B (Groq)' },
                    { id: 'llama-3.1-8b-instant', label: 'Llama 3.1 8B Instant (Groq)' },
                ];
            }

            return [
                {
                    key: 'provider',
                    label: 'AI Provider',
                    type: 'dropdown',
                    required: true,
                    options: [
                        { id: 'gemini', label: 'Google Gemini (Platform Default / Connected Key)' },
                        { id: 'openai', label: 'OpenAI (Requires Connected Account)' },
                        { id: 'groq', label: 'Groq (Requires Connected Account)' },
                    ],
                    default: 'gemini',
                    helpText: 'Select AI provider: Google Gemini (default / platform key), OpenAI (BYOK), or Groq (BYOK).'
                },
                {
                    key: 'model',
                    label: 'Model',
                    type: 'dropdown',
                    options: modelOptions,
                    default: currentProvider === 'openai' ? 'gpt-4o' : (currentProvider === 'groq' ? 'llama-3.3-70b-versatile' : 'gemini-2.5-flash'),
                    helpText: 'The LLM used for multi-step reasoning and function calling.'
                },
                {
                    key: 'systemPrompt',
                    label: 'System Prompt / Instructions',
                    type: 'textarea',
                    required: true,
                    placeholder: "Instructions defining the agent's role, rules, and how it should use tools and respond...",
                    default: "You are a helpful AI assistant. Analyze the incoming data and dynamically choose the appropriate tools to accomplish the goal.",
                    helpText: "Guides the agent's reasoning loop and tool selection behavior."
                },
                {
                    key: 'prompt',
                    label: 'User Prompt / Input Data',
                    type: 'textarea',
                    required: false,
                    placeholder: 'Input or instructions passed to the agent (e.g. {{steps.1.data}})...',
                    default: '{{steps.1.data}}',
                    helpText: 'The main request or trigger data passed into the agent from previous steps.'
                },
                {
                    key: 'temperature',
                    label: 'Temperature (0.0 - 1.0)',
                    type: 'number',
                    placeholder: '0.7',
                    default: 0.7,
                    helpText: 'Controls randomness (0.0 = deterministic, 1.0 = creative).'
                },
                {
                    key: 'maxIterations',
                    label: 'Max Reasoning Iterations',
                    type: 'number',
                    placeholder: '10',
                    default: 10,
                    helpText: 'Maximum number of ReAct reasoning loops before stopping.'
                },
                {
                    key: 'returnIntermediateSteps',
                    label: 'Include Reasoning & Tool Observations',
                    type: 'boolean',
                    default: true,
                    helpText: "When enabled, output includes the agent's step-by-step reasoning logs."
                },
            ];
        }
        if (!appDetail || !actionOrTriggerKey) return [];
        const listKey = isTrigger ? 'triggers' : 'actions';
        const keyField = isTrigger ? 'triggerKey' : 'actionKey';
        const items = appDetail[listKey] || [];
        const match = items.find((i) => i[keyField] === actionOrTriggerKey);
        return parseConfigSchema(match?.configSchema || {});
    }, [appDetail, actionOrTriggerKey, isTrigger, isAgentNode, data.configuration]);

    // ── Auto-seed schema defaults into data.configuration if not already set ──
    useEffect(() => {
        if (!configSchema || configSchema.length === 0) return;
        const currentConfig = data.configuration || {};
        let hasNewDefaults = false;
        const nextConfig = { ...currentConfig };

        configSchema.forEach((f) => {
            if (nextConfig[f.key] === undefined || nextConfig[f.key] === null || nextConfig[f.key] === '') {
                if (f.default !== undefined && f.default !== null && f.default !== '') {
                    nextConfig[f.key] = f.default;
                    hasNewDefaults = true;
                } else if (f.type === 'select' && f.options?.length > 0 && f.required) {
                    const firstOpt = typeof f.options[0] === 'string' ? f.options[0] : (f.options[0]?.value ?? f.options[0]?.id);
                    if (firstOpt !== undefined && firstOpt !== '') {
                        nextConfig[f.key] = firstOpt;
                        hasNewDefaults = true;
                    }
                } else if ((f.type === 'datetime' || f.type === 'date') && f.required) {
                    nextConfig[f.key] = '{{now}}';
                    hasNewDefaults = true;
                }
            }
        });

        if (hasNewDefaults) {
            updateNodeData(configNode.id, { configuration: nextConfig });
        }
    }, [configSchema, configNode.id, actionOrTriggerKey]);

    // ── Detect if app needs auth (connection) ──
    const selectedCatalogApp = (catalogApps || []).find((a) => a.appKey === data.appKey);
    const isNoAuthApp = selectedCatalogApp?.authType === 'NONE';

    // ── Tab completion state ──
    // For platform-key apps (Telegram, Gemini, Sarvam), default to ADMIN_KEY (managed) when not set
    const isUsingAdminKey = appDetail?.hasPlatformKey && (data.credentialSource === 'ADMIN_KEY' || !data.credentialSource);
    const isGeminiAgent = isAgentNode && (!data.configuration?.provider || data.configuration.provider === 'gemini');
    const hasConnection = isNoAuthApp || isGeminiAgent || !!data.connectionId || isUsingAdminKey;
    const setupComplete = !!(data.appKey && hasConnection && actionOrTriggerKey);

    const isFieldFilled = (f) => {
        if (!f.required) return true;
        const val = (data.configuration || {})[f.key] ?? f.default;
        return val != null && String(val).trim() !== '';
    };

    const configComplete = setupComplete && (
        configSchema.length === 0 || configSchema.every(isFieldFilled)
    );

    const progressPercent = useMemo(() => {
        let percent = 0;
        if (data.appKey && hasConnection) percent += 25;
        if (actionOrTriggerKey) percent += 25;
        if (setupComplete) {
            const requiredFields = configSchema.filter(f => f.required);
            if (requiredFields.length === 0) {
                percent += 50;
            } else {
                const filledCount = requiredFields.filter(isFieldFilled).length;
                percent += Math.round((filledCount / requiredFields.length) * 50);
            }
        }
        return Math.min(100, Math.max(0, percent));
    }, [data.appKey, hasConnection, actionOrTriggerKey, setupComplete, configSchema, data.configuration]);

    // ── Previous step variables for data passing ──
    const previousStepVariables = useMemo(() => {
        if (!allNodes || !configNode) return [];
        const currentStepIndex = configNode.data?.stepIndex;
        // With 1-based indexing: trigger=1, first action=2
        // Only steps >= 2 can reference previous steps
        if (currentStepIndex == null || currentStepIndex < 2) return [];

        return allNodes
            .filter((n) => {
                const si = n.data?.stepIndex;
                return si != null && si < currentStepIndex && n.data?.appKey;
            })
            .sort((a, b) => (a.data.stepIndex || 0) - (b.data.stepIndex || 0))
            .map((n) => {
                const nd = n.data;
                const isTriggerStep = n.type === 'trigger';
                const appMeta = (catalogApps || []).find((a) => a.appKey === nd.appKey);
                const appName = appMeta?.name || nd.appName || nd.appKey;
                const opName = nd.triggerName || nd.actionName
                    || nd.triggerKey || nd.actionKey || (isTriggerStep ? 'Trigger' : 'Action');
                const fields = getOutputFieldsForApp(nd.appKey, isTriggerStep);
                return {
                    stepIndex: nd.stepIndex,
                    appKey: nd.appKey,
                    appName,
                    operationName: opName,
                    fields,
                };
            });
    }, [allNodes, configNode, catalogApps]);

    // ── Config update helper ──
    const updateConfig = (key, value) => {
        const newConfig = { ...(data.configuration || {}), [key]: value };
        configSchema.forEach((field) => {
            if (field.dependsOn.includes(key) && newConfig[field.key] !== undefined) {
                delete newConfig[field.key];
            }
        });
        updateNodeData(configNode.id, { configuration: newConfig });
    };

    // ── Connection options for SearchableSelect ──
    const relevantConnections = useMemo(() => {
        if (isAgentNode && Array.isArray(allConnections) && allConnections.length > 0) {
            const aiKeys = ['agent', 'gemini', 'openai', 'groq'];
            const aiConns = allConnections.filter((c) => aiKeys.includes(c.appKey));
            return aiConns.length > 0 ? aiConns : selectedConnections;
        }
        return selectedConnections;
    }, [isAgentNode, allConnections, selectedConnections]);

    const connectionOptions = relevantConnections.map((c) => ({
        id: c.id,
        label: c.name || c.appKey,
        description: c.accountEmail || c.accountDisplayName || `Provider: ${c.appKey}`,
    }));

    // ── App options — show ALL catalog apps, not just connected ones ──
    const appOptions = (catalogApps || []).map((a) => ({
        id: a.appKey,
        label: a.name || a.appKey,
        description: a.category || null,
        iconUrl: a.iconUrl || null,
    }));

    // Extract granted scopes from the active connection
    const activeConnection = selectedConnections.find((c) => c.id === data.connectionId);
    const grantedScopes = activeConnection?.grantedScopes ? activeConnection.grantedScopes.split(/[\s,]+/).filter(Boolean) : null;

    // ── Trigger/Action options ──
    const operationOptions = (isTrigger ? selectedTriggerOptions : selectedActionOptions).map((opt) => {
        const kf = isTrigger ? 'triggerKey' : 'actionKey';
        let isGreyedOut = false;
        let tooltip = null;

        if (opt.requiredScopes && grantedScopes) {
            const missingScopes = opt.requiredScopes.filter((s) => !grantedScopes.includes(s));
            if (missingScopes.length > 0) {
                isGreyedOut = true;
                tooltip = `Missing required scopes: ${missingScopes.join(', ')}. Please reconnect your account and grant these permissions.`;
            }
        }

        return {
            id: opt[kf] || opt.name || '',
            label: opt.name || opt[kf] || '',
            description: opt.description || null,
            disabled: isGreyedOut,
            tooltip: tooltip,
        };
    });

    // Handle app selection (including via browser modal)
    const handleAppSelect = async (appKey) => {
        const appMeta = (catalogApps || []).find((a) => a.appKey === appKey);
        updateNodeData(configNode.id, {
            appKey,
            app: appKey,
            appName: appMeta?.name || appKey,
            iconUrl: appMeta?.iconUrl || null,
            connectionId: null,
            actionKey: '',
            triggerKey: '',
            triggerType: '',
            triggerName: '',
            actionName: '',
            action: '',
            configuration: {},
        });
        await ensureAppDetail(appKey);
    };

    // Handle action/trigger selection
    const handleOperationSelect = (key) => {
        const optionsList = isTrigger ? selectedTriggerOptions : selectedActionOptions;
        const keyField = isTrigger ? 'triggerKey' : 'actionKey';
        const match = optionsList.find((o) => o[keyField] === key);
        const name = match?.name || key;
        const appMeta = (catalogApps || []).find((a) => a.appKey === data.appKey);
        const appName = appMeta?.name || data.appKey;

        if (isTrigger) {
            updateNodeData(configNode.id, {
                triggerKey: key, actionKey: key, triggerType: key,
                triggerName: name, label: `${appName} · ${name}`,
                configuration: {},
            });
        } else {
            updateNodeData(configNode.id, {
                actionKey: key, action: key, actionName: name,
                label: `${appName} · ${name}`, configuration: {},
            });
        }
    };

    // Handle OAuth connect inline or open connection modal for API key apps
    const handleNewConnection = async (reconnectConnectionId) => {
        if (!data.appKey) return;
        if (appDetail?.authType === 'OAUTH2') {
            try {
                const { authorizationUrl } = await appCatalogApi.getOAuthUrl(data.appKey, reconnectConnectionId || undefined);
                if (authorizationUrl) {
                    window.open(authorizationUrl, '_blank', 'width=600,height=700');
                    return;
                }
            } catch {
                // fall through to connection modal
            }
        }
        if (onOpenAppBrowser) {
            onOpenAppBrowser(data.appKey);
        }
    };

    // Close account menu on outside click
    useEffect(() => {
        if (!accountMenuOpen) return;
        const handler = (e) => { if (accountMenuRef.current && !accountMenuRef.current.contains(e.target)) setAccountMenuOpen(false); };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [accountMenuOpen]);

    // Derive step display name
    const displayStepName = data.stepLabel || data.triggerName || data.actionName || data.appName || (isTrigger ? 'Configure Trigger' : 'Configure Action');
    const stepNumber = (nodeIndex ?? -1) + 1;

    const stepHeaderLogo = appDetail?.logoUrl || data.iconUrl || (data.appKey ? `/icons/${data.appKey}.svg` : null);

    return (
        <div className="cpb-container">
            {/* ── Header (Zapier-style) ── */}
            <div className="canvas-config-header">
                <div className="canvas-config-header-left">
                    {(stepHeaderLogo || data.appKey) && (
                        <img
                            src={stepHeaderLogo}
                            alt=""
                            className="canvas-config-header-icon app-logo-img"
                            onError={(e) => { e.target.style.display = 'none'; }}
                        />
                    )}
                    {editingName ? (
                        <input
                            className="cpb-name-input"
                            autoFocus
                            value={stepName}
                            onChange={e => setStepName(e.target.value)}
                            onBlur={() => {
                                if (stepName.trim()) updateNodeData(configNode.id, { stepLabel: stepName.trim() });
                                setEditingName(false);
                            }}
                            onKeyDown={e => { if (e.key === 'Enter') e.target.blur(); if (e.key === 'Escape') setEditingName(false); }}
                        />
                    ) : (
                        <span className="canvas-config-title" onClick={() => { setStepName(displayStepName); setEditingName(true); }}>
                            {stepNumber > 0 ? `${stepNumber}. ` : ''}{displayStepName}
                            <HiOutlinePencil className="cpb-name-edit-icon" />
                        </span>
                    )}
                </div>
                <div className="canvas-config-header-right">
                    {nodeCount > 1 && (
                        <div className="canvas-config-nav">
                            <button
                                className="canvas-config-nav-btn"
                                disabled={nodeIndex <= 0}
                                onClick={() => onNavigate?.('prev')}
                                title="Previous step"
                                aria-label="Previous step"
                            >
                                ‹
                            </button>
                            <span className="canvas-config-nav-label">Step {nodeIndex + 1} of {nodeCount}</span>
                            <button
                                className="canvas-config-nav-btn"
                                disabled={nodeIndex >= nodeCount - 1}
                                onClick={() => onNavigate?.('next')}
                                title="Next step"
                                aria-label="Next step"
                            >
                                ›
                            </button>
                        </div>
                    )}
                    <button
                        className="canvas-config-close"
                        onClick={onClose}
                        title="Close panel (Esc)"
                        aria-label="Close configuration panel"
                    >
                        <HiX />
                    </button>
                </div>
            </div>

            {/* ── Progress Banner ── */}
            <div className="cpb-progress-banner" style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '10px 20px', background: 'var(--bg-elevated, rgba(255, 255, 255, 0.03))', borderBottom: '1px solid var(--border-subtle, rgba(255,255,255,0.08))', borderTop: '1px solid var(--border-subtle, rgba(255,255,255,0.08))' }}>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px' }}>
                    <span style={{ fontSize: '0.74rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.5px', color: 'var(--text-secondary)' }}>
                        Configuration Progress
                    </span>
                    <span style={{ fontSize: '0.84rem', color: 'var(--text-primary)', fontWeight: 500 }}>
                        {configComplete ? 'All configuration complete! Ready to execute.' :
                         setupComplete ? 'Step 2: Fill required configuration fields.' :
                         'Step 1: Connect your account & select event.'}
                    </span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <AnimatedCircularProgressBar
                        max={100}
                        min={0}
                        value={progressPercent}
                        size={48}
                        strokeWidth={7}
                        gaugePrimaryColor="var(--text-primary, #ffffff)"
                        gaugeSecondaryColor="var(--border-secondary, rgba(255, 255, 255, 0.15))"
                    />
                </div>
            </div>

            {/* ── Stepper Tabs ── */}
            <div className="cpb-stepper">
                {TABS.map((tab, idx) => {
                    const isComplete = idx === 0 ? setupComplete : idx === 1 ? configComplete : (data.tested || !!data.lastTestResult);
                    const isCurrent = activeTab === idx;
                    const isAccessible = idx === 0 || (idx === 1 && setupComplete) || (idx === 2 && setupComplete);

                    return (
                        <button
                            key={tab}
                            className={`cpb-step ${isCurrent ? 'active' : ''} ${isComplete ? 'complete' : ''} ${!isAccessible ? 'disabled' : ''}`}
                            onClick={() => isAccessible && setActiveTab(idx)}
                            disabled={!isAccessible}
                            title={isAccessible ? `Go to Step ${idx + 1}: ${tab}` : `Complete previous steps to access ${tab}`}
                            aria-label={`Step ${idx + 1}: ${tab}`}
                        >
                            <span className="cpb-step-indicator">
                                {isComplete ? <HiCheck /> : idx + 1}
                            </span>
                            <span className="cpb-step-label">{tab}</span>
                            {idx < TABS.length - 1 && <span className="cpb-step-separator">›</span>}
                        </button>
                    );
                })}
            </div>

            {/* ── Tab Content ── */}
            <div className="cpb-tab-content">
                {/* ═══ SETUP TAB ═══ */}
                {activeTab === 0 && (
                    <div className="cpb-section">
                        {/* App selector — locked badge for agent nodes, clickable browser for all others */}
                        <div className="cpb-field">
                            <label className="cpb-label">{isTrigger ? 'Trigger App' : 'Action App'}</label>
                            {isAgentNode ? (
                                <div className="cpb-app-select-btn selected" style={{ cursor: 'default', pointerEvents: 'none' }}>
                                    <div className="cpb-app-select-icon">
                                        <img
                                            src={data.iconUrl || '/icons/agent.svg'}
                                            alt="AI Agent"
                                            className="app-logo-img"
                                            onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }}
                                        />
                                        <HiOutlineBolt style={{ display: 'none' }} />
                                    </div>
                                    <div className="cpb-app-select-text">
                                        <div className="cpb-app-select-name">AI Agent</div>
                                        <div className="cpb-app-select-hint">Autonomous reasoning node</div>
                                    </div>
                                </div>
                            ) : (
                                <button
                                    type="button"
                                    className={`cpb-app-select-btn ${data.appKey ? 'selected' : ''}`}
                                    onClick={() => onOpenAppBrowser && onOpenAppBrowser(null)}
                                    title={data.appKey ? `Change app (currently ${data.appName || data.appKey})` : 'Choose an app…'}
                                >
                                    <div className="cpb-app-select-icon">
                                        {data.appKey ? (
                                            <img src={appDetail?.logoUrl || data.iconUrl || `/icons/${data.appKey}.svg`} alt={data.appName || ''} className="app-logo-img" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }} />
                                        ) : (
                                            <HiOutlineBolt />
                                        )}
                                        <HiOutlineBolt style={{ display: 'none' }} />
                                    </div>
                                    <div className="cpb-app-select-text">
                                        <div className="cpb-app-select-name">
                                            {data.appName || data.appKey || 'Choose an app…'}
                                        </div>
                                        <div className="cpb-app-select-hint">
                                            {data.appKey ? 'Click to change app' : 'Browse all available apps'}
                                        </div>
                                    </div>
                                    <HiChevronRight className="cpb-app-select-chevron" />
                                </button>
                            )}
                        </div>

                        {/* Account — card with options menu */}
                        {data.appKey && !isNoAuthApp && (
                            <div className="cpb-field">
                                <label className="cpb-label">Account <span className="cpb-required">*</span></label>

                                {/* Platform-key apps (Telegram, Gemini, Sarvam): show managed card by default */}
                                {appDetail?.hasPlatformKey && (data.credentialSource === 'ADMIN_KEY' || !data.credentialSource) ? (
                                    <>
                                        <div className="cpb-account-card">
                                            <div className="cpb-account-left">
                                                <div className="cpb-app-select-icon" style={{ width: '28px', height: '28px' }}>
                                                    <img src={appDetail?.logoUrl || data.iconUrl || `/icons/${data.appKey}.svg`} alt="" className="app-logo-img" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }} />
                                                    <HiOutlineBolt style={{ display: 'none' }} />
                                                </div>
                                                <div className="cpb-account-info">
                                                    <span className="cpb-account-name">
                                                        {data.appKey === 'telegram' ? '@crescendo_app_bot' : `Crescendo ${data.appName || data.appKey}`}
                                                    </span>
                                                    <span className="cpb-account-hint">Managed · No setup needed</span>
                                                </div>
                                            </div>
                                            <div className="cpb-account-actions">
                                                <div className="cpb-account-menu-wrap" ref={accountMenuRef}>
                                                    <button type="button" className="cpb-account-dots" title="Account options" aria-label="Account options" onClick={() => setAccountMenuOpen(!accountMenuOpen)}>⋮</button>
                                                    {accountMenuOpen && (
                                                        <div className="cpb-account-menu">
                                                            <button type="button" onClick={() => {
                                                                setAccountMenuOpen(false);
                                                                updateNodeData(configNode.id, { credentialSource: 'PERSONAL', connectionId: null, account: null, accountName: '' });
                                                                if (onOpenAppBrowser) onOpenAppBrowser(data.appKey);
                                                            }}>
                                                                Use my own {data.appKey === 'telegram' ? 'bot token' : 'API key'}
                                                            </button>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </div>

                                        {/* Telegram: quick authorization links */}
                                        {data.appKey === 'telegram' && (
                                            <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', marginTop: '8px' }}>
                                                <div style={{ fontSize: '0.73rem', color: 'var(--text-tertiary)' }}>
                                                    Authorize the bot in your chat, group, or channel:
                                                </div>
                                                <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                                                    <a
                                                        href="https://t.me/crescendo_app_bot"
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        style={{
                                                            padding: '5px 10px',
                                                            borderRadius: '6px',
                                                            border: '1px solid var(--border-primary)',
                                                            background: 'var(--bg-elevated)',
                                                            color: 'var(--text-primary)',
                                                            textDecoration: 'none',
                                                            fontSize: '0.74rem',
                                                            fontWeight: 500,
                                                            display: 'inline-flex',
                                                            alignItems: 'center',
                                                            gap: '4px',
                                                        }}
                                                    >
                                                        Direct Chat ↗
                                                    </a>
                                                    <a
                                                        href="https://t.me/crescendo_app_bot?startgroup=true"
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        style={{
                                                            padding: '5px 10px',
                                                            borderRadius: '6px',
                                                            border: '1px solid var(--border-primary)',
                                                            background: 'var(--bg-elevated)',
                                                            color: 'var(--text-primary)',
                                                            textDecoration: 'none',
                                                            fontSize: '0.74rem',
                                                            fontWeight: 500,
                                                            display: 'inline-flex',
                                                            alignItems: 'center',
                                                            gap: '4px',
                                                        }}
                                                    >
                                                        Add to Group ↗
                                                    </a>
                                                    <a
                                                        href="https://t.me/crescendo_app_bot?startchannel&admin=post_messages+edit_messages+delete_messages+pin_messages"
                                                        target="_blank"
                                                        rel="noopener noreferrer"
                                                        style={{
                                                            padding: '5px 10px',
                                                            borderRadius: '6px',
                                                            border: '1px solid var(--border-primary)',
                                                            background: 'var(--bg-elevated)',
                                                            color: 'var(--text-primary)',
                                                            textDecoration: 'none',
                                                            fontSize: '0.74rem',
                                                            fontWeight: 500,
                                                            display: 'inline-flex',
                                                            alignItems: 'center',
                                                            gap: '4px',
                                                        }}
                                                    >
                                                        Add to Channel ↗
                                                    </a>
                                                </div>
                                            </div>
                                        )}
                                    </>
                                ) : data.connectionId && selectedConnections.length > 0 ? (() => {
                                    const conn = selectedConnections.find(c => c.id === data.connectionId);
                                    return (
                                        <div className="cpb-account-card">
                                            <div className="cpb-account-left">
                                                <div className="cpb-app-select-icon" style={{ width: '28px', height: '28px' }}>
                                                    <img src={appDetail?.logoUrl || data.iconUrl || `/icons/${data.appKey}.svg`} alt="" className="app-logo-img" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }} />
                                                    <HiOutlineBolt style={{ display: 'none' }} />
                                                </div>
                                                <div className="cpb-account-info">
                                                    <span className="cpb-account-name">{conn?.accountEmail || conn?.accountDisplayName || conn?.name || data.appName}</span>
                                                    {conn?.name && <span className="cpb-account-hint">{conn.name}</span>}
                                                </div>
                                            </div>
                                            <div className="cpb-account-actions">
                                                <button type="button" className="cpb-account-change" title="Change or switch account" aria-label="Change account" onClick={() => {
                                                    if (appDetail?.hasPlatformKey) {
                                                        updateNodeData(configNode.id, { credentialSource: 'ADMIN_KEY', connectionId: null, account: null, accountName: '' });
                                                    } else {
                                                        updateNodeData(configNode.id, { connectionId: null, account: null, accountName: '' });
                                                    }
                                                }}>Change</button>
                                                <div className="cpb-account-menu-wrap" ref={accountMenuRef}>
                                                    <button type="button" className="cpb-account-dots" title="Connection options" aria-label="Connection options" onClick={() => setAccountMenuOpen(!accountMenuOpen)}>⋮</button>
                                                    {accountMenuOpen && (
                                                        <div className="cpb-account-menu">
                                                            <button type="button" onClick={async () => {
                                                                setAccountMenuOpen(false);
                                                                const addToast = useToastStore.getState().addToast;
                                                                addToast('Testing connection…', 'info', 5000);
                                                                try {
                                                                    const result = await connectionsApi.test(data.connectionId);
                                                                    if (result.success) {
                                                                        addToast(result.message || 'Connection works!', 'success');
                                                                    } else {
                                                                        addToast(result.message || 'Connection test failed', 'error', 5000);
                                                                    }
                                                                } catch (err) {
                                                                    addToast('Test failed: ' + (err?.response?.data?.message || err.message), 'error', 5000);
                                                                }
                                                            }}>
                                                                Test connection
                                                            </button>
                                                            <button type="button" onClick={() => { setAccountMenuOpen(false); handleNewConnection(data.connectionId); }}>
                                                                Reconnect
                                                            </button>
                                                            {appDetail?.hasPlatformKey && (
                                                                <button type="button" onClick={() => {
                                                                    setAccountMenuOpen(false);
                                                                    updateNodeData(configNode.id, { credentialSource: 'ADMIN_KEY', connectionId: null, account: null, accountName: '' });
                                                                }}>
                                                                    Switch to managed account
                                                                </button>
                                                            )}
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })() : (
                                    <>
                                        {selectedConnections.length > 0 ? (
                                            <SearchableSelect
                                                options={connectionOptions}
                                                value={data.connectionId || ''}
                                                onChange={(id) => {
                                                    const conn = selectedConnections.find((c) => c.id === id);
                                                    updateNodeData(configNode.id, {
                                                        connectionId: id, account: id,
                                                        accountName: conn?.name || '',
                                                    });
                                                }}
                                                placeholder="Select account…"
                                                searchable={false}
                                            />
                                        ) : (
                                            <div className="cpb-empty-connections">
                                                <p>No accounts connected for <strong>{data.appName || data.appKey}</strong></p>
                                            </div>
                                        )}
                                        <button type="button" className={`cpb-browse-btn ${selectedConnections.length === 0 ? 'cpb-connect-cta' : ''}`} onClick={() => handleNewConnection()}>
                                            <HiPlus style={{ fontSize: '0.7rem' }} />
                                            {selectedConnections.length === 0 ? `Connect ${data.appName || data.appKey}` : 'Connect new account'}
                                        </button>
                                    </>
                                )}
                            </div>
                        )}

                        {/* No-auth app notice */}
                        {data.appKey && isNoAuthApp && (
                            <div className="cpb-field">
                                <div className="cpb-no-auth-notice">
                                    <HiCheck style={{ color: '#22c55e', fontSize: '0.85rem' }} />
                                    <span>No account connection needed</span>
                                </div>
                            </div>
                        )}

                        {/* Action/Trigger selector — locked badge for agent nodes, dropdown for all others */}
                        {data.appKey && hasConnection && (
                            <div className="cpb-field">
                                <label className="cpb-label">{isTrigger ? 'Trigger Event' : 'Action'}</label>
                                {isAgentNode ? (
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-secondary)', borderRadius: '6px', color: 'var(--text-primary)', fontSize: '0.875rem' }}>
                                        <HiCheck style={{ color: '#22c55e', flexShrink: 0 }} />
                                        <span>AI Agent — autonomous reasoning &amp; tool selection</span>
                                    </div>
                                ) : (
                                    <SearchableSelect
                                        options={operationOptions}
                                        value={actionOrTriggerKey || ''}
                                        onChange={handleOperationSelect}
                                        placeholder={isTrigger ? 'Choose trigger…' : 'Choose action…'}
                                    />
                                )}
                            </div>
                        )}

                        {/* Webhook setup panel */}
                        {isTrigger && (
                            <WebhookSetupPanel
                                appKey={data.appKey}
                                iconUrl={data.iconUrl || appDetail?.logoUrl}
                                configuration={data.configuration}
                                webhookInfo={webhookInfo}
                                workflowId={workflowId}
                            />
                        )}

                        {/* Continue button */}
                        {setupComplete && (
                            <button
                                type="button"
                                className="cpb-continue-btn"
                                onClick={() => setActiveTab(1)}
                            >
                                Continue
                            </button>
                        )}
                    </div>
                )}

                {/* ═══ CONFIGURE TAB ═══ */}
                {activeTab === 1 && (
                    <div className="cpb-section">
                        {configSchema.length === 0 ? (
                            <div className="cpb-empty-config">
                                No configuration needed for this {isTrigger ? 'trigger' : 'action'}.
                            </div>
                        ) : (
                            configSchema.map((field) => (
                                <div key={field.key} className="cpb-field">
                                    <label className="cpb-label">
                                        {field.label}
                                        {field.required && <span className="cpb-required">*</span>}
                                    </label>
                                    <DynamicField
                                        field={field}
                                        appKey={data.appKey}
                                        connectionId={data.connectionId}
                                        credentialSource={data.credentialSource}
                                        config={data.configuration || {}}
                                        value={(data.configuration || {})[field.key]}
                                        onChange={(val) => updateConfig(field.key, val)}
                                        availableVariables={previousStepVariables}
                                    />
                                    {field.helpText && (
                                        <span className="cpb-help">{field.helpText}</span>
                                    )}
                                </div>
                            ))
                        )}

                        {/* Continue to Test */}
                        <button
                            type="button"
                            className="cpb-continue-btn"
                            onClick={() => setActiveTab(2)}
                        >
                            Continue
                        </button>
                    </div>
                )}

                {/* ═══ TEST TAB ═══ */}
                {activeTab === 2 && (
                    <div className="cpb-section">
                        <div className="cpb-test-intro">
                            {isTrigger
                                ? 'Test your trigger to find recent records from your connected account.'
                                : 'Test your action to verify it works with the configuration above.'}
                        </div>
                        <TestResultPanel
                            appKey={data.appKey}
                            actionKey={data.actionKey}
                            connectionId={data.connectionId}
                            configuration={data.configuration || {}}
                            isTrigger={isTrigger}
                        />
                    </div>
                )}
            </div>

            {/* ── Footer ── */}
            <div className="canvas-config-footer">
                {isTrigger ? (
                    // Trigger nodes: clear contents but keep node position
                    <button
                        className="canvas-config-btn"
                        title="Clear this trigger's configuration"
                        onClick={onClear || onDelete}
                        style={{ color: 'var(--text-tertiary)' }}
                    >
                        <HiOutlineTrash />
                        <span style={{ fontSize: '0.72rem', marginLeft: 4 }}>Clear</span>
                    </button>
                ) : (
                    <button className="canvas-config-btn danger" title="Remove this action step" onClick={onDelete}>
                        <HiOutlineTrash />
                    </button>
                )}
                <button
                    className="canvas-config-btn canvas-config-btn-save"
                    title="Save step configuration and close panel"
                    aria-label="Save and close configuration"
                    onClick={() => {
                    const appMeta = catalogApps.find(a => a.appKey === data.appKey);
                    const appName = appMeta?.name || data.appName || data.appKey;
                    if (!isTrigger && data.appKey && data.actionKey) {
                        const actionName = selectedActionOptions.find(a => a.actionKey === data.actionKey)?.name || data.actionKey;
                        updateNodeData(configNode.id, { label: `${appName} · ${actionName}`, actionName });
                    } else if (isTrigger && data.appKey && (data.triggerKey || data.actionKey)) {
                        const triggerKey = data.triggerKey || data.actionKey;
                        const triggerName = selectedTriggerOptions.find(t => t.triggerKey === triggerKey)?.name || triggerKey;
                        updateNodeData(configNode.id, { label: `${appName} · ${triggerName}`, triggerName });
                    }
                    // Persist to backend via parent's handleSave, then close
                    if (onSaveAndClose) {
                        onSaveAndClose();
                    } else {
                        onClose?.();
                    }
                }}>
                    <HiCheck style={{ fontSize: '1rem' }} /> Save & Close
                </button>
            </div>
        </div>
    );
}
