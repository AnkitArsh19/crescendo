import { useEffect } from 'react';

const DEFAULT_TITLE = 'Crescendo: Workflow Automation Platform';
const DEFAULT_DESC =
  'Crescendo is a workflow automation platform that helps you build, orchestrate, and monitor complex workflows with an intuitive visual builder. Connect Google Sheets, Gmail, Discord, Slack, and AI agents.';

function setMetaTag(attr, key, val) {
  if (!val) return;
  let el = document.querySelector(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(attr, key);
    document.head.appendChild(el);
  }
  el.setAttribute('content', val);
}

/**
 * usePageMeta: sets document.title and Open Graph / Twitter meta tags per page.
 *
 * @param {string} title Plain page title (e.g. "Workflows"). Gets formatted as "Workflows | Crescendo".
 * @param {string} [description] Optional meta description override.
 */
export default function usePageMeta(title, description) {
  useEffect(() => {
    let formatted = DEFAULT_TITLE;
    if (title) {
      formatted = title.includes('Crescendo') ? title : `${title} | Crescendo`;
    }
    document.title = formatted;

    const desc = description || DEFAULT_DESC;
    const currentUrl = typeof window !== 'undefined' ? window.location.href : 'https://app.crescendo.run/';

    setMetaTag('name', 'description', desc);
    setMetaTag('property', 'og:title', formatted);
    setMetaTag('name', 'twitter:title', formatted);
    setMetaTag('property', 'og:description', desc);
    setMetaTag('name', 'twitter:description', desc);
    setMetaTag('property', 'og:url', currentUrl);
    setMetaTag('name', 'twitter:url', currentUrl);

    return () => {
      document.title = DEFAULT_TITLE;
      setMetaTag('name', 'description', DEFAULT_DESC);
      setMetaTag('property', 'og:title', DEFAULT_TITLE);
      setMetaTag('name', 'twitter:title', DEFAULT_TITLE);
      setMetaTag('property', 'og:description', DEFAULT_DESC);
      setMetaTag('name', 'twitter:description', DEFAULT_DESC);
      setMetaTag('property', 'og:url', 'https://app.crescendo.run/');
      setMetaTag('name', 'twitter:url', 'https://app.crescendo.run/');
    };
  }, [title, description]);
}
