import { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlineDeviceMobile, HiOutlineX, HiOutlineArrowsExpand } from 'react-icons/hi';
import './RotateLandscapePrompt.css';

export default function RotateLandscapePrompt({
    title = 'Rotate to Landscape',
    message = 'For the best experience and layout on this page, please rotate your device to landscape.',
    showRotateBtn = true,
}) {
    const [isVisible, setIsVisible] = useState(false);

    useEffect(() => {
        if (typeof window === 'undefined') return;

        // Trigger on initial open if user is on a mobile device in portrait mode
        const isMobile = window.innerWidth <= 768;
        const isPortrait = window.innerHeight > window.innerWidth;

        if (isMobile && isPortrait) {
            setIsVisible(true);

            // Auto dismiss after 8 seconds so it doesn't obstruct the user
            const timer = setTimeout(() => {
                setIsVisible(false);
            }, 8000);

            // If the user rotates the device to landscape, dismiss immediately
            const handleOrientation = () => {
                if (window.innerWidth > window.innerHeight) {
                    setIsVisible(false);
                }
            };

            window.addEventListener('resize', handleOrientation);
            window.addEventListener('orientationchange', handleOrientation);

            return () => {
                clearTimeout(timer);
                window.removeEventListener('resize', handleOrientation);
                window.removeEventListener('orientationchange', handleOrientation);
            };
        }
    }, []);

    const handleDismiss = useCallback(() => {
        setIsVisible(false);
    }, []);

    const handleRotate = useCallback(async () => {
        setIsVisible(false);
        try {
            const el = document.documentElement;
            if (el.requestFullscreen) {
                await el.requestFullscreen();
            } else if (el.webkitRequestFullscreen) {
                await el.webkitRequestFullscreen();
            }
            if (screen.orientation && screen.orientation.lock) {
                await screen.orientation.lock('landscape').catch(() => {});
            }
        } catch (err) {
            console.warn('Orientation lock / fullscreen not allowed by browser:', err);
        }
    }, []);

    return (
        <AnimatePresence>
            {isVisible && (
                <motion.div
                    className="rotate-prompt-container"
                    initial={{ opacity: 0, y: 30, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: 20, scale: 0.95 }}
                    transition={{ duration: 0.28, ease: [0.22, 1, 0.36, 1] }}
                    role="alert"
                    aria-live="polite"
                >
                    <div className="rotate-prompt-card">
                        <button
                            className="rotate-prompt-close"
                            onClick={handleDismiss}
                            aria-label="Dismiss rotation tip"
                        >
                            <HiOutlineX />
                        </button>

                        <div className="rotate-prompt-content">
                            <div className="rotate-prompt-icon-wrap">
                                <HiOutlineDeviceMobile className="rotate-prompt-phone-icon" />
                            </div>
                            <div className="rotate-prompt-text">
                                <h4 className="rotate-prompt-title">{title}</h4>
                                <p className="rotate-prompt-desc">{message}</p>
                            </div>
                        </div>

                        <div className="rotate-prompt-actions">
                            {showRotateBtn && (
                                <button
                                    className="rotate-prompt-btn-rotate"
                                    onClick={handleRotate}
                                >
                                    <HiOutlineArrowsExpand /> Rotate Screen
                                </button>
                            )}
                            <button
                                className="rotate-prompt-btn-dismiss"
                                onClick={handleDismiss}
                            >
                                Continue in Portrait
                            </button>
                        </div>
                    </div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
