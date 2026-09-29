import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { FaShieldAlt } from 'react-icons/fa';
import './CookieConsent.css';

export default function CookieConsent() {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        try {
            const consent = localStorage.getItem('crescendo_cookie_consent');
            if (!consent) {
                // Short timeout so page mounts smoothly before banner slides up
                const timer = setTimeout(() => setVisible(true), 1200);
                return () => clearTimeout(timer);
            }
        } catch {
            // In case localStorage is blocked in incognito/strict iframe mode
        }
    }, []);

    const handleAccept = () => {
        try {
            localStorage.setItem('crescendo_cookie_consent', 'true');
        } catch {}
        setVisible(false);
    };

    if (!visible) return null;

    return (
        <aside className="cookie-consent-banner" aria-label="Cookie and Privacy Consent">
            <div className="cookie-consent-header">
                <FaShieldAlt className="cookie-consent-icon" style={{ color: '#a78bfa' }} />
                <h3 className="cookie-consent-title">Privacy &amp; Cookie Preferences</h3>
            </div>
            <p className="cookie-consent-body">
                We use strictly essential first-party cookies for secure authentication and workspace state.
                We do not track you across other websites or deploy third-party advertising cookies. Read our{' '}
                <Link to="/cookies" onClick={() => setVisible(false)}>
                    Cookie Policy
                </Link>{' '}
                to learn more.
            </p>
            <div className="cookie-consent-actions">
                <Link to="/cookies" className="cookie-btn-link" onClick={() => setVisible(false)}>
                    Preferences
                </Link>
                <button type="button" className="cookie-btn-accept" onClick={handleAccept}>
                    Accept Essential
                </button>
            </div>
        </aside>
    );
}
