import { useEffect, useState, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import {
  HiOutlinePlus, HiOutlineTrash, HiOutlinePencil,
  HiOutlineTemplate, HiOutlineX, HiOutlineUpload,
  HiOutlineBadgeCheck, HiOutlineDocumentText,
  HiOutlineFilter,
} from 'react-icons/hi';
import { templatesApi } from '../../api/emailServiceApi';
import TemplateBlockEditor from './TemplateBlockEditor';
import './Settings.css';

const STARTER_TEMPLATES = [
  {
    id: 'welcome-onboarding',
    name: 'Welcome & Getting Started',
    category: 'Transactional',
    badge: 'Onboarding',
    badgeColor: '#6366f1',
    subject: 'Welcome to Crescendo, {{FIRST_NAME}}',
    description: 'Modern product welcome with quick-start checklist, primary CTA, and help center links.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.4);">
        <!-- Top accent banner -->
        <tr><td style="background:linear-gradient(90deg, #6366f1, #a855f7);height:4px;"></td></tr>
        <tr><td style="padding:40px 36px 20px 36px;">
          <span style="display:inline-block;padding:4px 10px;background:#27272a;color:#a1a1aa;border-radius:20px;font-size:12px;font-weight:600;letter-spacing:0.5px;text-transform:uppercase;margin-bottom:16px;">Welcome</span>
          <h1 style="color:#ffffff;font-size:26px;font-weight:700;line-height:1.25;margin:0 0 16px 0;">Welcome aboard, {{FIRST_NAME}}!</h1>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 24px 0;">We're thrilled to have you join our automation community. Crescendo gives you the superpower to connect apps, schedule workflows, and orchestrate AI agents seamlessly.</p>
          
          <!-- Checklist Card -->
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#27272a;border-radius:8px;padding:20px;margin-bottom:28px;">
            <tr><td style="color:#ffffff;font-weight:600;font-size:14px;padding-bottom:12px;">3 steps to get started:</td></tr>
            <tr><td style="color:#d4d4d8;font-size:14px;padding:4px 0;">✦ <strong>Connect your apps</strong> — Authenticate GitHub, Slack, Discord or Gemini in 1 click.</td></tr>
            <tr><td style="color:#d4d4d8;font-size:14px;padding:4px 0;">✦ <strong>Pick a workflow starter</strong> — Launch automated pipelines with pre-built DAGs.</td></tr>
            <tr><td style="color:#d4d4d8;font-size:14px;padding:4px 0;">✦ <strong>Activate your automation</strong> — Let Crescendo handle the repeat busywork 24/7.</td></tr>
          </table>

          <!-- Button -->
          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 28px auto;">
            <tr><td align="center" style="background:linear-gradient(135deg, #6366f1, #4f46e5);border-radius:8px;padding:12px 28px;">
              <a href="{{DASHBOARD_URL}}" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;display:inline-block;">Open Your Dashboard &rarr;</a>
            </td></tr>
          </table>

          <p style="color:#71717a;font-size:13px;line-height:1.5;margin:0;">Have questions? Reply directly to this email or visit our documentation anytime.</p>
        </td></tr>
        <tr><td style="background:#121215;padding:20px 36px;border-top:1px solid #27272a;text-align:center;">
          <p style="color:#52525b;font-size:12px;margin:0 0 6px 0;">&copy; {{CURRENT_YEAR}} {{COMPANY_NAME}}. All rights reserved.</p>
          <a href="{{CRESCENDO_UNSUBSCRIBE_URL}}" style="color:#71717a;font-size:12px;text-decoration:underline;">Unsubscribe preferences</a>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'b1', type: 'badge', text: 'Welcome', color: '#a1a1aa', bgColor: '#27272a', align: 'left', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'b2', type: 'heading', content: 'Welcome aboard, {{FIRST_NAME}}!', level: 'h1', align: 'left', color: '#ffffff', fontSize: 26, fontWeight: '700', lineHeight: 125, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'b3', type: 'text', content: "We're thrilled to have you join our automation community. Crescendo gives you the superpower to connect apps, schedule workflows, and orchestrate AI agents seamlessly.", align: 'left', color: '#a1a1aa', fontSize: 15, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'b4', type: 'text', content: "✦ Connect your apps in 1 click\n✦ Pick a workflow starter\n✦ Activate your automation to run 24/7", align: 'left', color: '#d4d4d8', fontSize: 14, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 24, left: 0 } },
      { id: 'b5', type: 'button', text: 'Open Your Dashboard →', url: '{{DASHBOARD_URL}}', align: 'center', bgColor: '#6366f1', textColor: '#ffffff', fontSize: 15, fontWeight: '600', paddingX: 28, paddingY: 12, borderRadius: 8, fullWidth: false, margin: { top: 0, right: 0, bottom: 24, left: 0 } },
      { id: 'b6', type: 'divider', color: '#27272a', thickness: 1, style: 'solid', margin: { top: 16, right: 0, bottom: 16, left: 0 } },
      { id: 'b7', type: 'unsubscribe', text: 'You received this email because you signed up for Crescendo.', linkText: 'Unsubscribe here', url: '{{CRESCENDO_UNSUBSCRIBE_URL}}', align: 'center', color: '#52525b', fontSize: 12, margin: { top: 16, right: 0, bottom: 8, left: 0 } }
    ]
  },
  {
    id: 'devops-alert',
    name: 'CI/CD & Server Outage Alert',
    category: 'Developer',
    badge: 'Critical Alert',
    badgeColor: '#ef4444',
    subject: '🚨 Build Failure: {{WORKFLOW_NAME}} on {{BRANCH_NAME}}',
    description: 'Terminal-style diagnostic alert with branch, commit SHA, and direct log link.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #3f1818;border-radius:12px;overflow:hidden;box-shadow:0 8px 24px rgba(0,0,0,0.5);">
        <tr><td style="background:#ef4444;height:4px;"></td></tr>
        <tr><td style="padding:36px 36px 20px 36px;">
          <span style="display:inline-block;padding:4px 10px;background:#451a1a;color:#fca5a5;border-radius:20px;font-size:12px;font-weight:700;letter-spacing:0.5px;text-transform:uppercase;margin-bottom:16px;">● Pipeline Failed</span>
          <h1 style="color:#ffffff;font-size:24px;font-weight:700;line-height:1.3;margin:0 0 14px 0;">Execution error in {{WORKFLOW_NAME}}</h1>
          <p style="color:#a1a1aa;font-size:14px;line-height:1.5;margin:0 0 20px 0;">A step in your automated pipeline exited with a non-zero status code on branch <code style="color:#fca5a5;background:#27272a;padding:2px 6px;border-radius:4px;">{{BRANCH_NAME}}</code>.</p>
          
          <!-- Terminal / Diagnostic Box -->
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#09090b;border:1px solid #27272a;border-radius:8px;padding:16px;margin-bottom:24px;font-family:'Commit Mono',monospace;">
            <tr><td style="color:#71717a;font-size:12px;padding-bottom:8px;">// Commit: {{COMMIT_SHA}} &bull; Runner ID: {{RUN_ID}}</td></tr>
            <tr><td style="color:#f87171;font-size:13px;line-height:1.4;white-space:pre-wrap;">{{ERROR_MESSAGE}}</td></tr>
          </table>

          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
            <tr><td style="background:#ef4444;border-radius:6px;padding:10px 22px;">
              <a href="{{LOGS_URL}}" style="color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">Inspect Full Logs &rarr;</a>
            </td></tr>
          </table>
          <p style="color:#71717a;font-size:12px;margin:0;">Automatic retries remaining: 0. Downstream dependent nodes have been paused.</p>
        </td></tr>
        <tr><td style="background:#121215;padding:16px 36px;border-top:1px solid #27272a;text-align:center;">
          <p style="color:#52525b;font-size:12px;margin:0;">Alert dispatched by Crescendo Monitoring &bull; <a href="{{ALERT_SETTINGS_URL}}" style="color:#71717a;">Mute alert</a></p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'd1', type: 'badge', text: 'Pipeline Failed', color: '#fca5a5', bgColor: '#451a1a', align: 'left', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'd2', type: 'heading', content: 'Execution error in {{WORKFLOW_NAME}}', level: 'h1', align: 'left', color: '#ffffff', fontSize: 24, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 14, left: 0 } },
      { id: 'd3', type: 'text', content: 'A step in your pipeline exited with an error on branch {{BRANCH_NAME}} (Commit: {{COMMIT_SHA}}).', align: 'left', color: '#a1a1aa', fontSize: 14, fontWeight: '400', lineHeight: 155, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'd4', type: 'quote', content: 'Error diagnostic:\n{{ERROR_MESSAGE}}', align: 'left', color: '#f87171', fontSize: 13, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'd5', type: 'button', text: 'Inspect Full Logs →', url: '{{LOGS_URL}}', align: 'left', bgColor: '#ef4444', textColor: '#ffffff', fontSize: 14, fontWeight: '600', paddingX: 20, paddingY: 10, borderRadius: 6, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'd6', type: 'divider', color: '#27272a', thickness: 1, style: 'solid', margin: { top: 12, right: 0, bottom: 12, left: 0 } },
      { id: 'd7', type: 'unsubscribe', text: 'Alert dispatched by Crescendo Monitoring.', linkText: 'Configure alerts', url: '{{ALERT_SETTINGS_URL}}', align: 'center', color: '#52525b', fontSize: 12, margin: { top: 8, right: 0, bottom: 8, left: 0 } }
    ]
  },
  {
    id: 'student-invite',
    name: 'Student Project & Hackathon Invite',
    category: 'Student',
    badge: 'Collaboration',
    badgeColor: '#10b981',
    subject: "You're invited to join {{PROJECT_NAME}} on Crescendo",
    description: 'Vibrant study group or hackathon team invitation with deadline badge and join CTA.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:linear-gradient(90deg, #10b981, #06b6d4);height:4px;"></td></tr>
        <tr><td style="padding:36px;">
          <span style="display:inline-block;padding:4px 10px;background:#064e3b;color:#6ee7b7;border-radius:20px;font-size:12px;font-weight:600;margin-bottom:16px;">🎓 Team Workspace</span>
          <h1 style="color:#ffffff;font-size:24px;font-weight:700;margin:0 0 12px 0;">{{INVITER_NAME}} invited you to {{PROJECT_NAME}}</h1>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px 0;">Hi {{FIRST_NAME}}, you have been invited to collaborate on <strong>{{PROJECT_NAME}}</strong>. Join your teammates to build automated data pipelines, review LeetCode algorithms, and build AI projects together.</p>
          
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#27272a;border-radius:8px;padding:16px;margin-bottom:24px;">
            <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">📅 <strong>Target Date / Milestone:</strong> {{DEADLINE_DATE}}</td></tr>
            <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">👥 <strong>Role:</strong> Contributor &bull; Full Pipeline Access</td></tr>
          </table>

          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 24px auto;">
            <tr><td style="background:#10b981;border-radius:8px;padding:12px 28px;">
              <a href="{{JOIN_URL}}" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;">Join Team Workspace &rarr;</a>
            </td></tr>
          </table>
          <p style="color:#71717a;font-size:12px;text-align:center;margin:0;">This invitation was sent by {{INVITER_NAME}} ({{INVITER_EMAIL}}).</p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 's1', type: 'badge', text: 'Team Workspace', color: '#6ee7b7', bgColor: '#064e3b', align: 'left', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 's2', type: 'heading', content: '{{INVITER_NAME}} invited you to {{PROJECT_NAME}}', level: 'h1', align: 'left', color: '#ffffff', fontSize: 24, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 14, left: 0 } },
      { id: 's3', type: 'text', content: 'Hi {{FIRST_NAME}}, join your teammates to collaborate on automated data pipelines, daily coding challenges, and AI workflows.', align: 'left', color: '#a1a1aa', fontSize: 15, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 's4', type: 'button', text: 'Join Team Workspace →', url: '{{JOIN_URL}}', align: 'center', bgColor: '#10b981', textColor: '#ffffff', fontSize: 15, fontWeight: '600', paddingX: 28, paddingY: 12, borderRadius: 8, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 's5', type: 'divider', color: '#27272a', thickness: 1, style: 'solid', margin: { top: 12, right: 0, bottom: 12, left: 0 } },
      { id: 's6', type: 'unsubscribe', text: 'Invitation sent via Crescendo Workspace.', linkText: 'Decline invitation', url: '{{DECLINE_URL}}', align: 'center', color: '#52525b', fontSize: 12, margin: { top: 8, right: 0, bottom: 8, left: 0 } }
    ]
  },
  {
    id: 'ai-newsletter',
    name: 'AI Tech Radar & Weekly Newsletter',
    category: 'Marketing',
    badge: 'Newsletter',
    badgeColor: '#8b5cf6',
    subject: 'Tech Pulse #{{ISSUE_NUMBER}}: {{NEWSLETTER_HEADLINE}}',
    description: 'Clean editorial layout with AI summaries, topic tags, and article deep dives.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:linear-gradient(90deg, #8b5cf6, #ec4899);height:4px;"></td></tr>
        <tr><td style="padding:36px;">
          <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;">
            <span style="font-size:12px;font-weight:700;color:#c084fc;text-transform:uppercase;letter-spacing:1px;">TECH RADAR &bull; ISSUE #{{ISSUE_NUMBER}}</span>
            <span style="font-size:12px;color:#71717a;">{{READ_TIME}} min read</span>
          </div>
          <h1 style="color:#ffffff;font-size:26px;font-weight:700;margin:0 0 16px 0;">{{NEWSLETTER_HEADLINE}}</h1>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 24px 0;">Hi {{FIRST_NAME}}, here are the biggest breakthroughs in AI agent orchestration, developer tooling, and distributed automation this week.</p>
          
          <!-- AI Executive Summary Callout -->
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#20182b;border-left:4px solid #8b5cf6;border-radius:4px;padding:16px;margin-bottom:24px;">
            <tr><td style="color:#e9d5ff;font-weight:600;font-size:13px;padding-bottom:6px;">✨ AI Executive Summary:</td></tr>
            <tr><td style="color:#d8b4fe;font-size:14px;line-height:1.5;">{{AI_SUMMARY_TEXT}}</td></tr>
          </table>

          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 24px 0;">
            <tr><td style="background:#8b5cf6;border-radius:6px;padding:10px 24px;">
              <a href="{{ARTICLE_LINK}}" style="color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;">Read Full Analysis &rarr;</a>
            </td></tr>
          </table>
        </td></tr>
        <tr><td style="background:#121215;padding:20px 36px;border-top:1px solid #27272a;text-align:center;">
          <p style="color:#52525b;font-size:12px;margin:0 0 6px 0;">Curated weekly with ❤️ by Crescendo.</p>
          <a href="{{CRESCENDO_UNSUBSCRIBE_URL}}" style="color:#71717a;font-size:12px;text-decoration:underline;">Unsubscribe from this newsletter</a>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'n1', type: 'badge', text: 'Issue #{{ISSUE_NUMBER}}', color: '#c084fc', bgColor: '#2e1065', align: 'left', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'n2', type: 'heading', content: '{{NEWSLETTER_HEADLINE}}', level: 'h1', align: 'left', color: '#ffffff', fontSize: 26, fontWeight: '700', lineHeight: 125, margin: { top: 0, right: 0, bottom: 14, left: 0 } },
      { id: 'n3', type: 'text', content: 'Hi {{FIRST_NAME}}, here are the biggest breakthroughs in AI agent orchestration and distributed automation this week.', align: 'left', color: '#a1a1aa', fontSize: 15, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'n4', type: 'quote', content: '✨ AI Executive Summary:\n{{AI_SUMMARY_TEXT}}', align: 'left', color: '#d8b4fe', fontSize: 14, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'n5', type: 'button', text: 'Read Full Analysis →', url: '{{ARTICLE_LINK}}', align: 'left', bgColor: '#8b5cf6', textColor: '#ffffff', fontSize: 14, fontWeight: '600', paddingX: 22, paddingY: 10, borderRadius: 6, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'n6', type: 'divider', color: '#27272a', thickness: 1, style: 'solid', margin: { top: 12, right: 0, bottom: 12, left: 0 } },
      { id: 'n7', type: 'unsubscribe', text: 'Curated weekly with ❤️ by Crescendo.', linkText: 'Unsubscribe', url: '{{CRESCENDO_UNSUBSCRIBE_URL}}', align: 'center', color: '#52525b', fontSize: 12, margin: { top: 8, right: 0, bottom: 8, left: 0 } }
    ]
  },
  {
    id: 'order-receipt',
    name: 'Itemized Billing & Order Receipt',
    category: 'Transactional',
    badge: 'Receipt',
    badgeColor: '#3b82f6',
    subject: 'Receipt for order #{{ORDER_ID}}',
    description: 'Clean minimalist receipt with itemized breakdown, tax info, and PDF invoice link.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:#3b82f6;height:4px;"></td></tr>
        <tr><td style="padding:36px;">
          <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0 0 8px 0;">Payment Receipt</h1>
          <p style="color:#a1a1aa;font-size:14px;margin:0 0 24px 0;">Invoice <strong>#{{ORDER_ID}}</strong> &bull; Billed on {{BILLING_DATE}}</p>
          
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;margin-bottom:20px;">
            <tr style="border-bottom:1px solid #27272a;"><td style="padding:10px 0;color:#71717a;font-size:12px;text-transform:uppercase;">Description</td><td style="padding:10px 0;color:#71717a;font-size:12px;text-transform:uppercase;text-align:right;">Amount</td></tr>
            <tr style="border-bottom:1px solid #27272a;"><td style="padding:14px 0;color:#e4e4e7;font-size:14px;">{{PLAN_NAME}} (Monthly Subscription)</td><td style="padding:14px 0;color:#ffffff;font-size:14px;font-weight:600;text-align:right;">\${{AMOUNT_PAID}}</td></tr>
            <tr><td style="padding:16px 0 0 0;color:#a1a1aa;font-size:14px;"><strong>Total Paid:</strong></td><td style="padding:16px 0 0 0;color:#3b82f6;font-size:18px;font-weight:700;text-align:right;">\${{AMOUNT_PAID}}</td></tr>
          </table>

          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 0 20px 0;">
            <tr><td style="background:#27272a;border-radius:6px;padding:8px 18px;">
              <a href="{{INVOICE_URL}}" style="color:#e4e4e7;text-decoration:none;font-size:13px;font-weight:600;">Download PDF Invoice &rarr;</a>
            </td></tr>
          </table>
        </td></tr>
        <tr><td style="background:#121215;padding:16px 36px;border-top:1px solid #27272a;text-align:center;">
          <p style="color:#52525b;font-size:12px;margin:0;">Billed to {{EMAIL}}. Need help? Contact billing@crescendo.run.</p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'r1', type: 'heading', content: 'Payment Receipt #{{ORDER_ID}}', level: 'h1', align: 'left', color: '#ffffff', fontSize: 22, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 8, left: 0 } },
      { id: 'r2', type: 'text', content: 'Hi {{FIRST_NAME}}, thank you for your payment on {{BILLING_DATE}}. Your invoice is processed.', align: 'left', color: '#a1a1aa', fontSize: 14, fontWeight: '400', lineHeight: 155, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'r3', type: 'quote', content: 'Plan: {{PLAN_NAME}}\nAmount: ${{AMOUNT_PAID}} USD\nStatus: Paid ✓', align: 'left', color: '#60a5fa', fontSize: 14, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'r4', type: 'button', text: 'Download PDF Invoice →', url: '{{INVOICE_URL}}', align: 'left', bgColor: '#27272a', textColor: '#ffffff', fontSize: 13, fontWeight: '600', paddingX: 18, paddingY: 8, borderRadius: 6, fullWidth: false, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'r5', type: 'unsubscribe', text: 'Billed to {{EMAIL}}.', linkText: 'Billing settings', url: 'https://app.crescendo.run/settings/billing', align: 'center', color: '#52525b', fontSize: 12, margin: { top: 8, right: 0, bottom: 8, left: 0 } }
    ]
  },
  {
    id: 'security-otp',
    name: 'Security Alert & Passkey / OTP Code',
    category: 'Developer',
    badge: 'Security',
    badgeColor: '#f59e0b',
    subject: '{{OTP_CODE}} is your Crescendo verification code',
    description: 'High-visibility monospace security token with device/IP login context and 10m expiry.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:#f59e0b;height:4px;"></td></tr>
        <tr><td style="padding:36px;text-align:center;">
          <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0 0 12px 0;">Your Verification Code</h1>
          <p style="color:#a1a1aa;font-size:14px;margin:0 0 24px 0;">Use the code below to complete your authentication. Never share this code with anyone.</p>
          
          <!-- Big Monospace OTP Box -->
          <div style="background:#09090b;border:1px solid #3f3f46;border-radius:8px;padding:16px 28px;display:inline-block;margin-bottom:24px;">
            <span style="font-family:'Commit Mono',monospace;font-size:32px;font-weight:700;letter-spacing:6px;color:#fbbf24;">{{OTP_CODE}}</span>
          </div>

          <p style="color:#71717a;font-size:13px;line-height:1.5;margin:0 0 16px 0;">⏰ Valid for 10 minutes &bull; Requested from {{DEVICE_NAME}} ({{LOCATION}}).</p>
          <p style="color:#ef4444;font-size:12px;margin:0;"><a href="{{SECURITY_URL}}" style="color:#f87171;">Didn't request this? Revoke session immediately &rarr;</a></p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'o1', type: 'badge', text: 'Authentication Code', color: '#fcd34d', bgColor: '#451a03', align: 'center', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'o2', type: 'heading', content: 'Your One-Time Code', level: 'h1', align: 'center', color: '#ffffff', fontSize: 24, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 8, left: 0 } },
      { id: 'o3', type: 'quote', content: '{{OTP_CODE}}', align: 'center', color: '#fbbf24', fontSize: 28, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'o4', type: 'text', content: 'Valid for 10 minutes. Requested from {{DEVICE_NAME}} in {{LOCATION}}.', align: 'center', color: '#a1a1aa', fontSize: 13, fontWeight: '400', lineHeight: 155, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
      { id: 'o5', type: 'button', text: 'Secure My Account →', url: '{{SECURITY_URL}}', align: 'center', bgColor: '#27272a', textColor: '#f87171', fontSize: 13, fontWeight: '600', paddingX: 18, paddingY: 8, borderRadius: 6, fullWidth: false, margin: { top: 0, right: 0, bottom: 16, left: 0 } }
    ]
  },
  {
    id: 'event-webinar',
    name: 'Workshop & Webinar Confirmation',
    category: 'Student',
    badge: 'Event Pass',
    badgeColor: '#06b6d4',
    subject: 'Confirmed: Your seat for {{EVENT_NAME}} is reserved!',
    description: 'Speaker spotlight, calendar date badge, interactive add-to-calendar and Zoom links.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:linear-gradient(90deg, #06b6d4, #3b82f6);height:4px;"></td></tr>
        <tr><td style="padding:36px;">
          <span style="display:inline-block;padding:4px 10px;background:#083344;color:#67e8f9;border-radius:20px;font-size:12px;font-weight:600;margin-bottom:14px;">🎟 Registration Confirmed</span>
          <h1 style="color:#ffffff;font-size:24px;font-weight:700;margin:0 0 12px 0;">You're attending {{EVENT_NAME}}</h1>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px 0;">Hi {{FIRST_NAME}}, your registration is locked in! Join industry leaders and fellow developers for this hands-on workshop.</p>
          
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#27272a;border-radius:8px;padding:16px;margin-bottom:24px;">
            <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">📅 <strong>Date & Time:</strong> {{EVENT_DATE_TIME}}</td></tr>
            <tr><td style="color:#e4e4e7;font-size:13px;padding:4px 0;">🎙 <strong>Speaker:</strong> {{SPEAKER_NAME}}</td></tr>
          </table>

          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 20px auto;">
            <tr><td style="background:#06b6d4;border-radius:8px;padding:12px 28px;">
              <a href="{{MEETING_URL}}" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;">Join Live Stream / Meeting &rarr;</a>
            </td></tr>
          </table>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'w1', type: 'badge', text: 'Registration Confirmed', color: '#67e8f9', bgColor: '#083344', align: 'left', margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'w2', type: 'heading', content: "You're attending {{EVENT_NAME}}", level: 'h1', align: 'left', color: '#ffffff', fontSize: 24, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'w3', type: 'text', content: 'Hi {{FIRST_NAME}}, your seat is reserved. Hosted by {{SPEAKER_NAME}} on {{EVENT_DATE_TIME}}.', align: 'left', color: '#a1a1aa', fontSize: 15, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'w4', type: 'button', text: 'Join Live Stream →', url: '{{MEETING_URL}}', align: 'center', bgColor: '#06b6d4', textColor: '#ffffff', fontSize: 15, fontWeight: '600', paddingX: 28, paddingY: 12, borderRadius: 8, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } }
    ]
  },
  {
    id: 'customer-feedback',
    name: 'Customer Feedback & NPS Survey',
    category: 'Marketing',
    badge: 'Feedback',
    badgeColor: '#ec4899',
    subject: 'How was your experience, {{FIRST_NAME}}? (2-min survey)',
    description: 'Personal note from founder with rating scale and feedback question.',
    contentHtml: `<!DOCTYPE html>
<html>
<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
<body style="margin:0;padding:0;background-color:#09090b;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#f4f4f5;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color:#09090b;padding:40px 16px;">
    <tr><td align="center">
      <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color:#18181b;border:1px solid #27272a;border-radius:12px;overflow:hidden;">
        <tr><td style="background:#ec4899;height:4px;"></td></tr>
        <tr><td style="padding:36px;">
          <h1 style="color:#ffffff;font-size:22px;font-weight:700;margin:0 0 14px 0;">Help us build a better Crescendo</h1>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 20px 0;">Hi {{FIRST_NAME}},</p>
          <p style="color:#a1a1aa;font-size:15px;line-height:1.6;margin:0 0 24px 0;">How likely are you to recommend Crescendo to a friend or teammate?</p>
          
          <table role="presentation" cellpadding="0" cellspacing="0" style="margin:0 auto 24px auto;">
            <tr><td style="background:#ec4899;border-radius:8px;padding:12px 28px;">
              <a href="{{SURVEY_URL}}" style="color:#ffffff;text-decoration:none;font-size:15px;font-weight:600;">Take 2-Minute Survey &rarr;</a>
            </td></tr>
          </table>
          <p style="color:#71717a;font-size:13px;line-height:1.5;margin:0;">Every response is read directly by our founding engineering team.</p>
        </td></tr>
      </table>
    </td></tr>
  </table>
</body>
</html>`,
    blocks: [
      { id: 'f1', type: 'heading', content: 'Help us build a better Crescendo', level: 'h1', align: 'left', color: '#ffffff', fontSize: 22, fontWeight: '700', lineHeight: 130, margin: { top: 0, right: 0, bottom: 12, left: 0 } },
      { id: 'f2', type: 'text', content: 'Hi {{FIRST_NAME}},\n\nHow has your experience with Crescendo been? We would love 2 minutes of your candid thoughts.', align: 'left', color: '#a1a1aa', fontSize: 15, fontWeight: '400', lineHeight: 160, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
      { id: 'f3', type: 'button', text: 'Take 2-Minute Survey →', url: '{{SURVEY_URL}}', align: 'center', bgColor: '#ec4899', textColor: '#ffffff', fontSize: 15, fontWeight: '600', paddingX: 28, paddingY: 12, borderRadius: 8, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } }
    ]
  }
];

const CATEGORIES = ['All', 'Transactional', 'Developer', 'Student', 'Marketing'];

const RESERVED_VARIABLE_NAMES = new Set([
  'FIRST_NAME', 'LAST_NAME', 'EMAIL', 'COMPANY_NAME', 'CRESCENDO_UNSUBSCRIBE_URL', 'CURRENT_YEAR'
]);

function declaredVariablesFor(...content) {
  const names = new Set();
  content.join('\n').replace(/\{\{([A-Z][A-Z0-9_]*)\}\}/g, (_, name) => {
    if (!RESERVED_VARIABLE_NAMES.has(name)) names.add(name);
    return _;
  });
  return [...names].map((name) => ({ name, type: 'STRING', fallbackValue: null }));
}

export default function TemplatesSettings() {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(null); // null | template object
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [cloneModal, setCloneModal] = useState(false);
  const [broadcastId, setBroadcastId] = useState('');
  const [operationError, setOperationError] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('All');

  const fetchTemplates = async () => {
    setLoading(true);
    try { setTemplates(await templatesApi.list()); } catch { /* */ }
    setLoading(false);
  };

  useEffect(() => { fetchTemplates(); }, []);

  const filteredStarters = useMemo(() => {
    if (selectedCategory === 'All') return STARTER_TEMPLATES;
    return STARTER_TEMPLATES.filter((s) => s.category === selectedCategory);
  }, [selectedCategory]);

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await templatesApi.delete(deleteTarget);
      setTemplates(templates.filter((t) => t.id !== deleteTarget));
    } catch { /* */ }
    setDeleteTarget(null);
  };

  const handleCloneFromBroadcast = async () => {
    if (!broadcastId.trim()) return;
    try {
      const saved = await templatesApi.cloneFromBroadcast(broadcastId.trim());
      setTemplates(prev => [saved, ...prev]);
      setCloneModal(false);
      setBroadcastId('');
      setEditing(saved);
    } catch { /* */ }
  };

  const handleCreateNewTemplate = async () => {
    setOperationError('');
    try {
      const defaultDoc = {
        name: 'My First Template',
        subject: 'Welcome to our community',
        previewText: '',
        fromAddress: '',
        replyTo: '',
        htmlBody: '<!doctype html><html><body><p>Start building your email.</p></body></html>',
        textBody: '',
        editorDocument: JSON.stringify({
          blocks: [
            { id: crypto.randomUUID(), type: 'heading', content: 'Your Section Title', level: 'h1', align: 'left', color: '#18181b', fontSize: 26, fontWeight: '700', lineHeight: 130, letterSpacing: 0, margin: { top: 0, right: 0, bottom: 12, left: 0 } },
            { id: crypto.randomUUID(), type: 'text', content: 'Write your email body copy here. Use dynamic tags such as {{FIRST_NAME}} to personalize it.', align: 'left', color: '#3f3f46', fontSize: 15, fontWeight: '400', lineHeight: 155, margin: { top: 0, right: 0, bottom: 16, left: 0 } },
            { id: crypto.randomUUID(), type: 'button', text: 'Confirm Action', url: 'https://example.com/action', align: 'center', bgColor: '#18181b', textColor: '#ffffff', fontSize: 14, fontWeight: '600', paddingX: 20, paddingY: 10, borderRadius: 6, fullWidth: false, margin: { top: 0, right: 0, bottom: 20, left: 0 } },
            { id: crypto.randomUUID(), type: 'divider', color: '#e4e4e7', thickness: 1, style: 'solid', margin: { top: 24, right: 0, bottom: 24, left: 0 } },
            { id: crypto.randomUUID(), type: 'unsubscribe', text: 'You received this email because you signed up for updates.', linkText: 'Unsubscribe here', url: '{{CRESCENDO_UNSUBSCRIBE_URL}}', align: 'center', color: '#71717a', fontSize: 12, margin: { top: 32, right: 0, bottom: 16, left: 0 } }
          ]
        })
      };
      const created = await templatesApi.create(defaultDoc);
      setTemplates(prev => [created, ...prev]);
      setEditing(created);
    } catch (error) {
      setOperationError(error.response?.data?.message || 'We could not create the template. Please try again.');
    }
  };

  const handleUseStarterTemplate = async (starter) => {
    setOperationError('');
    try {
      const doc = {
        name: starter.name,
        subject: starter.subject,
        previewText: '',
        fromAddress: '',
        replyTo: '',
        htmlBody: starter.contentHtml,
        textBody: starter.contentHtml.replace(/<[^>]+>/g, ' '),
        variables: declaredVariablesFor(starter.subject, starter.contentHtml),
        editorDocument: JSON.stringify({
          hasHtmlOverride: false,
          blocks: (starter.blocks || []).map((b) => ({ ...b, id: crypto.randomUUID() }))
        })
      };
      const created = await templatesApi.create(doc);
      setTemplates(prev => [created, ...prev]);
      setEditing(created);
    } catch (error) {
      setOperationError(error.response?.data?.message || 'We could not create that starter template. Please try again.');
    }
  };

  return (
    <motion.div className="email-templates-page" initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }}>
      <div className="settings-section-header">
        <div>
          <h2 className="settings-section-title">Templates</h2>
          <p className="settings-section-desc">
            Build reusable emails with personal details such as <code>{'{{FIRST_NAME}}'}</code>, then publish when they are ready to send.
          </p>
        </div>
        <div className="email-template-actions">
          <button className="settings-btn-secondary" onClick={() => setCloneModal(true)}>
            <HiOutlineUpload /> Clone from Broadcast
          </button>
          <button className="settings-btn-primary" onClick={handleCreateNewTemplate}>
            <HiOutlinePlus /> New Template
          </button>
        </div>
      </div>

      {operationError && (
        <div className="settings-inline-error" role="alert">
          {operationError}
          <button type="button" onClick={() => setOperationError('')} aria-label="Dismiss">×</button>
        </div>
      )}

      {/* Starter Template Gallery */}
      <div style={{ margin: '24px 0 32px 0' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12, flexWrap: 'wrap', gap: 10 }}>
          <h3 style={{ fontSize: 13, fontWeight: 700, color: 'var(--text-secondary)', margin: 0, textTransform: 'uppercase', letterSpacing: 0.8 }}>
            Pre-made Starter Templates
          </h3>
          {/* Category Filter Pills */}
          <div style={{ display: 'flex', gap: 6 }}>
            {CATEGORIES.map((cat) => (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                style={{
                  fontSize: 12,
                  fontWeight: selectedCategory === cat ? 600 : 500,
                  padding: '4px 10px',
                  borderRadius: 20,
                  border: '1px solid',
                  borderColor: selectedCategory === cat ? 'var(--primary-color, #6366f1)' : 'var(--border-color)',
                  background: selectedCategory === cat ? 'rgba(99, 102, 241, 0.15)' : 'transparent',
                  color: selectedCategory === cat ? 'var(--text-primary, #ffffff)' : 'var(--text-secondary)',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                {cat}
              </button>
            ))}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 14 }}>
          {filteredStarters.map((starter) => (
            <motion.div
              key={starter.id}
              whileHover={{ y: -2, borderColor: 'var(--border-hover, #3f3f46)' }}
              transition={{ duration: 0.15 }}
              style={{
                background: 'var(--bg-secondary)',
                border: '1px solid var(--border-color)',
                borderRadius: 12,
                padding: 16,
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                position: 'relative',
                overflow: 'hidden'
              }}
              onClick={() => handleUseStarterTemplate(starter)}
            >
              <div>
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 8 }}>
                  <span style={{
                    fontSize: 11,
                    fontWeight: 700,
                    textTransform: 'uppercase',
                    letterSpacing: 0.5,
                    color: starter.badgeColor || '#6366f1',
                    background: `${starter.badgeColor || '#6366f1'}18`,
                    padding: '2px 8px',
                    borderRadius: 12
                  }}>
                    {starter.badge}
                  </span>
                  <span style={{ fontSize: 11, color: 'var(--text-tertiary, #71717a)' }}>{starter.category}</span>
                </div>
                <h4 style={{ margin: '0 0 6px 0', fontSize: 14, fontWeight: 600, color: 'var(--text-primary)' }}>{starter.name}</h4>
                <p style={{ margin: 0, fontSize: 12, color: 'var(--text-secondary)', lineHeight: 1.45 }}>{starter.description}</p>
              </div>
              <div style={{ marginTop: 14, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <span style={{ fontSize: 12, color: 'var(--primary-color, #6366f1)', fontWeight: 600, display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                  Use Starter &rarr;
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      </div>

      <h3 style={{ fontSize: 16, fontWeight: 600, color: 'var(--text-primary)', margin: '28px 0 16px 0' }}>Your Custom Templates</h3>

      {loading ? (
        <div className="settings-skeleton-list">{[1, 2].map((i) => <div key={i} className="settings-skeleton-row" />)}</div>
      ) : templates.length === 0 ? (
        <div className="settings-empty">
          <HiOutlineTemplate className="settings-empty-icon" />
          <h3>Create your first template</h3>
          <p>Start from a blank, responsive email and reuse it whenever you need it.</p>
          <button className="settings-btn-primary settings-empty-action" onClick={handleCreateNewTemplate}>
            <HiOutlinePlus /> Create template
          </button>
        </div>
      ) : (
        <div className="template-grid">
          {templates.map((t) => (
            <motion.div
              key={t.id}
              className="template-card"
              onClick={() => setEditing(t)}
              style={{ cursor: 'pointer' }}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
            >
              <div className="template-card-header">
                <h3>{t.name}</h3>
                <div className="template-card-actions">
                  <button
                    className="settings-icon-btn"
                    onClick={(e) => { e.stopPropagation(); setEditing(t); }}
                    title="Edit"
                  >
                    <HiOutlinePencil />
                  </button>
                  <button
                    className="settings-icon-btn settings-danger-icon"
                    onClick={(e) => { e.stopPropagation(); setDeleteTarget(t.id); }}
                    title="Delete"
                  >
                    <HiOutlineTrash />
                  </button>
                </div>
              </div>
              <p className="template-subject">{t.subject}</p>
              <div className="template-card-footer">
                <span className={`template-status-badge ${t.status === 'PUBLISHED' ? 'published' : 'draft'}`}>
                  {t.status === 'PUBLISHED' ? <HiOutlineBadgeCheck /> : <HiOutlineDocumentText />}
                  {t.status}
                </span>
                <span className="template-date">
                  {t.updatedAt ? new Date(t.updatedAt).toLocaleDateString() : new Date(t.createdAt).toLocaleDateString()}
                </span>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Full-screen Template Block Editor mounted to document.body */}
      {editing && createPortal(
        <TemplateBlockEditor
          template={editing}
          onClose={() => setEditing(null)}
          onSaved={(saved) => {
            setTemplates(prev => {
              const exists = prev.some(t => t.id === saved.id);
              if (exists) return prev.map(t => t.id === saved.id ? saved : t);
              return [saved, ...prev];
            });
            setEditing(saved);
          }}
        />,
        document.body
      )}

      {/* Delete Confirmation */}
      <AnimatePresence>
        {deleteTarget && (
          <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setDeleteTarget(null)}>
            <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
              <div className="conn-modal-header"><h2>Delete Template</h2></div>
              <div className="conn-modal-body"><p style={{ color: 'var(--text-secondary)' }}>This template will be permanently removed. Emails already sent are unaffected.</p></div>
              <div className="conn-modal-footer">
                <button className="conn-btn-secondary" onClick={() => setDeleteTarget(null)}>Cancel</button>
                <button className="conn-btn-danger" onClick={handleDelete}>Delete</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Clone from Broadcast modal */}
      <AnimatePresence>
        {cloneModal && (
          <motion.div className="conn-modal-backdrop" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={() => setCloneModal(false)}>
            <motion.div className="conn-modal conn-modal-sm" initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }} onClick={(e) => e.stopPropagation()}>
              <div className="conn-modal-header">
                <h2>Clone from Broadcast</h2>
                <button className="conn-modal-close" onClick={() => setCloneModal(false)}><HiOutlineX /></button>
              </div>
              <div className="conn-modal-body">
                <p style={{ color: 'var(--text-secondary)', fontSize: 13 }}>Paste the ID of an existing broadcast to copy its HTML content into a new draft template.</p>
                <label className="conn-form-label">
                  Broadcast ID
                  <input className="conn-form-input" value={broadcastId} onChange={e => setBroadcastId(e.target.value)} placeholder="e.g., 3fa85f64-5717-4562-b3fc-2c963f66afa6" />
                </label>
              </div>
              <div className="conn-modal-footer">
                <button className="conn-btn-secondary" onClick={() => setCloneModal(false)}>Cancel</button>
                <button className="conn-btn-primary" onClick={handleCloneFromBroadcast} disabled={!broadcastId.trim()}>Clone</button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
}
