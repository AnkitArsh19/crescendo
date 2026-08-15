import { useEffect } from 'react';

/**
 * usePageMeta — sets document.title and the <meta name="description"> per page.
 *
 * @param {string} title        Plain page title (e.g. "Workflows"). Gets formatted as "Workflows | Crescendo".
 * @param {string} [description] Optional meta description override.
 */
export default function usePageMeta(title, description) {
  useEffect(() => {
    const formatted = title ? `${title} | Crescendo` : 'Crescendo — Workflow Automation';
    document.title = formatted;

    let metaEl = document.querySelector('meta[name="description"]');
    if (metaEl && description) {
      metaEl.setAttribute('content', description);
    }

    return () => {
      // Reset to default on unmount so one forgotten call doesn't bleed
      document.title = 'Crescendo';
      if (metaEl) {
        metaEl.setAttribute(
          'content',
          'Crescendo is a workflow automation platform that helps you build, orchestrate, and monitor complex workflows with an intuitive visual builder.',
        );
      }
    };
  }, [title, description]);
}
