import { useState, useEffect } from 'react';
import { HiOutlineKey, HiX } from 'react-icons/hi';
import { useNavigate, useLocation } from 'react-router-dom';
import useAuthStore from '../store/authStore';
import api from '../api/axios';
import './PasskeyNudge.css'; // We will create this or use inline styles, let's use inline or standard classes

export default function PasskeyNudge() {
    const { user, isGuest } = useAuthStore();
    const navigate = useNavigate();
    const location = useLocation();
    const [isVisible, setIsVisible] = useState(false);

    // Determine if we should show the nudge
    useEffect(() => {
        if (!user || isGuest) return;

        // Condition 1: Must be immediately after login
        const justLoggedIn = location.state?.justLoggedIn;
        if (!justLoggedIn) return;

        // Condition 2: No passkeys registered
        if (user.passkeyCount && user.passkeyCount > 0) return;

        // Condition 3: Not opted out permanently
        if (user.passkeyNudgeOptedOut) return;

        // Condition 4: Max 2 dismissals
        if (user.passkeyNudgeDismissCount >= 2) return;

        // Condition 5: 14-day cooldown
        if (user.passkeyNudgeLastDismissedAt) {
            const lastDismissed = new Date(user.passkeyNudgeLastDismissedAt);
            const fourteenDaysAgo = new Date();
            fourteenDaysAgo.setDate(fourteenDaysAgo.getDate() - 14);
            if (lastDismissed > fourteenDaysAgo) return;
        }

        setIsVisible(true);
    }, [user, isGuest, location]);

    const handleDismiss = async (permanent) => {
        setIsVisible(false);
        try {
            await api.post('/users/me/passkey-nudge/dismiss', { permanent });
            // Update local state so it doesn't reappear on reload
            useAuthStore.setState(state => {
                if (state.user) {
                    return {
                        user: {
                            ...state.user,
                            passkeyNudgeOptedOut: permanent ? true : state.user.passkeyNudgeOptedOut,
                            passkeyNudgeDismissCount: (state.user.passkeyNudgeDismissCount || 0) + 1,
                            passkeyNudgeLastDismissedAt: new Date().toISOString()
                        }
                    };
                }
                return state;
            });
        } catch (error) {
            console.error('Failed to dismiss passkey nudge', error);
        }
    };

    if (!isVisible) return null;

    // Branched copy based on hasPassword
    const isOAuthOnly = user && !user.hasPassword;
    const title = isOAuthOnly ? 'Secure your account with a passkey' : 'Faster sign-in with passkeys';
    const description = isOAuthOnly 
        ? 'Add a passkey so you can sign in even if your Google or GitHub account is unavailable.' 
        : 'Set up a passkey to sign in safely with your fingerprint, face, or screen lock.';

    return (
        <div className="passkey-nudge-overlay">
            <div className="passkey-nudge-card">
                <button className="passkey-nudge-close" onClick={() => handleDismiss(false)} title="Dismiss">
                    <HiX />
                </button>
                <div className="passkey-nudge-icon">
                    <HiOutlineKey />
                </div>
                <h3>{title}</h3>
                <p>{description}</p>
                <div className="passkey-nudge-actions">
                    <button 
                        className="passkey-nudge-primary" 
                        onClick={() => {
                            setIsVisible(false);
                            navigate('/dashboard/settings/security');
                        }}
                    >
                        Set up passkey
                    </button>
                    <button className="passkey-nudge-secondary" onClick={() => handleDismiss(true)}>
                        Don't ask again
                    </button>
                </div>
            </div>
        </div>
    );
}
