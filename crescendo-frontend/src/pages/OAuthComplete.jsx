import { useEffect } from 'react';

/**
 * OAuthComplete — lightweight page that the OAuth popup redirects to.
 * 
 * Reads the base64-encoded result from the URL hash, posts it to the
 * opener window via postMessage, and closes the popup.
 * 
 * Because this page runs on the SAME origin as the main app,
 * window.close() is guaranteed to work.
 */
export default function OAuthComplete() {
    useEffect(() => {
        try {
            const hash = window.location.hash.substring(1); // remove #
            if (hash) {
                const json = atob(hash.replace(/-/g, '+').replace(/_/g, '/'));
                const data = JSON.parse(json);
                if (window.opener) {
                    window.opener.postMessage(data, window.location.origin);
                }
            }
        } catch (e) {
            console.warn('OAuth complete: failed to parse result', e);
        }

        // Close the popup
        window.close();

        // Fallback: if window.close() is blocked, redirect after 2s
        const timer = setTimeout(() => {
            window.location.href = '/dashboard/connections';
        }, 2000);

        return () => clearTimeout(timer);
    }, []);

    return (
        <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            height: '100vh',
            fontFamily: 'Inter, system-ui, sans-serif',
            color: '#888',
            background: '#0a0a0a',
        }}>
            <div style={{
                width: 32, height: 32, borderRadius: '50%',
                border: '3px solid #22c55e', borderTopColor: 'transparent',
                animation: 'spin 0.8s linear infinite',
                marginBottom: 16,
            }} />
            <p style={{ margin: 0 }}>Connected! Closing…</p>
            <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
    );
}
