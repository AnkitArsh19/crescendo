import { useState, useEffect, useCallback } from 'react';

/**
 * Cycles through phrases with a smooth typewriter + backspace effect.
 * @param {string[]} phrases - Array of phrases to cycle through.
 * @param {number}   typeSpeed   - Ms per character while typing forward.
 * @param {number}   backSpeed   - Ms per character while erasing.
 * @param {number}   pauseAfter  - Ms to pause once phrase is fully typed.
 */
export function useTypewriter(phrases, typeSpeed = 60, backSpeed = 30, pauseAfter = 2200) {
    const [displayText, setDisplayText] = useState('');
    const [phraseIndex, setPhraseIndex] = useState(0);
    const [charIndex, setCharIndex] = useState(0);
    const [isDeleting, setIsDeleting] = useState(false);
    const [isPaused, setIsPaused] = useState(false);

    const tick = useCallback(() => {
        const phrase = phrases[phraseIndex];

        if (isPaused) return;

        if (!isDeleting) {
            // Still typing forward
            if (charIndex < phrase.length) {
                setDisplayText(phrase.slice(0, charIndex + 1));
                setCharIndex(c => c + 1);
            } else {
                // Fully typed — pause before erasing
                setIsPaused(true);
                setTimeout(() => {
                    setIsPaused(false);
                    setIsDeleting(true);
                }, pauseAfter);
            }
        } else {
            // Erasing
            if (charIndex > 0) {
                setDisplayText(phrase.slice(0, charIndex - 1));
                setCharIndex(c => c - 1);
            } else {
                // Fully erased — move to next phrase
                setIsDeleting(false);
                setPhraseIndex(i => (i + 1) % phrases.length);
            }
        }
    }, [charIndex, isDeleting, isPaused, phraseIndex, phrases, pauseAfter]);

    useEffect(() => {
        const delay = isDeleting ? backSpeed : typeSpeed;
        const timer = setTimeout(tick, delay);
        return () => clearTimeout(timer);
    }, [tick, isDeleting, typeSpeed, backSpeed]);

    return { displayText, isTyping: !isDeleting && !isPaused };
}
