import { useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { HiSun, HiMoon } from 'react-icons/hi';
import { useTheme } from './ThemeContext';
import './ThemeToggle.css';

/**
 * Premium animated theme toggle button.
 * Uses Framer Motion for smooth, professional rotation and scale morphing between Sun and Moon.
 */
export default function ThemeToggle({ className = '', style = {}, title = null }) {
  const { theme, toggleTheme } = useTheme();

  const handleClick = useCallback((e) => {
    toggleTheme(e);
  }, [toggleTheme]);

  const isDark = theme === 'dark';
  const label = title || (isDark ? 'Switch to light theme' : 'Switch to dark theme');

  return (
    <button
      type="button"
      className={`crescendo-theme-toggle ${className}`}
      onClick={handleClick}
      title={label}
      aria-label={label}
      style={style}
    >
      <span
        id="theme-toggle-anchor"
        aria-hidden="true"
        style={{
          position: 'absolute',
          left: '50%',
          top: '50%',
          width: 0,
          height: 0,
          pointerEvents: 'none',
          opacity: 0,
          visibility: 'hidden',
        }}
      />
      <AnimatePresence mode="wait" initial={false}>
        <motion.span
          key={isDark ? 'sun' : 'moon'}
          className="theme-toggle-icon-inner"
          initial={{ rotate: -80, scale: 0.5, opacity: 0 }}
          animate={{ rotate: 0, scale: 1, opacity: 1 }}
          exit={{ rotate: 80, scale: 0.5, opacity: 0 }}
          transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
        >
          {isDark ? (
            <HiSun className="theme-toggle-svg" aria-hidden="true" />
          ) : (
            <HiMoon className="theme-toggle-svg" aria-hidden="true" />
          )}
        </motion.span>
      </AnimatePresence>
    </button>
  );
}
