import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';
import { flushSync } from 'react-dom';

const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('crescendo-theme');
    return saved || 'dark';
  });
  const isTransitioningRef = useRef(false);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('crescendo-theme', theme);
  }, [theme]);

  const toggleTheme = useCallback((arg) => {
    if (isTransitioningRef.current || document.documentElement.dataset.magicuiThemeVt === 'active') {
      return;
    }

    const nextTheme = theme === 'dark' ? 'light' : 'dark';
    const duration = 650;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let cx = null;
    let cy = null;

    // 1. Prioritize measuring the imaginary anchor object placed directly inside the theme button
    const anchor = document.getElementById('theme-toggle-anchor');
    if (anchor) {
      const rect = anchor.getBoundingClientRect();
      if (rect && (rect.top > 0 || rect.left > 0)) {
        cx = rect.left;
        cy = rect.top;
      }
    }

    // 2. Fallback to event target or bounding rect if anchor is not rendered (e.g. offscreen or dashboard)
    if (cx == null || cy == null || (cx === 0 && cy === 0)) {
      if (arg && arg.currentTarget && typeof arg.currentTarget.getBoundingClientRect === 'function') {
        const rect = arg.currentTarget.getBoundingClientRect();
        cx = rect.left + rect.width / 2;
        cy = rect.top + rect.height / 2;
      } else if (arg && typeof arg.x === 'number' && typeof arg.y === 'number') {
        cx = arg.x;
        cy = arg.y;
      } else {
        const btn = document.querySelector('.theme-toggle');
        if (btn) {
          const rect = btn.getBoundingClientRect();
          cx = rect.left + rect.width / 2;
          cy = rect.top + rect.height / 2;
        } else {
          cx = viewportWidth / 2;
          cy = viewportHeight / 2;
        }
      }
    }

    const maxRadius = Math.hypot(
      Math.max(cx, viewportWidth - cx),
      Math.max(cy, viewportHeight - cy)
    );

    const applyTheme = () => {
      setTheme(nextTheme);
    };

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (typeof document.startViewTransition !== 'function' || prefersReducedMotion) {
      applyTheme();
      return;
    }

    // Convert coordinates to percentages of the snapshot reference box to avoid Windows fractional scale (e.g. 125%/150%) positioning bugs (#989)
    const toX = (x) => `${(x / viewportWidth) * 100}%`;
    const toY = (y) => `${(y / viewportHeight) * 100}%`;
    const point = (x, y) => `${toX(x)} ${toY(y)}`;
    const toRadius = (r) => `${(r / (Math.hypot(viewportWidth, viewportHeight) / Math.SQRT2)) * 100}%`;

    const clipPath = [
      `circle(0% at ${point(cx, cy)})`,
      `circle(${toRadius(maxRadius)} at ${point(cx, cy)})`,
    ];

    const root = document.documentElement;
    root.dataset.magicuiThemeVt = 'active';
    root.style.setProperty('--magicui-theme-toggle-vt-duration', `${duration}ms`);
    root.style.setProperty('--magicui-theme-vt-clip-from', clipPath[0]);

    const cleanup = () => {
      isTransitioningRef.current = false;
      delete root.dataset.magicuiThemeVt;
      root.style.removeProperty('--magicui-theme-toggle-vt-duration');
      root.style.removeProperty('--magicui-theme-vt-clip-from');
    };

    isTransitioningRef.current = true;
    const transition = document.startViewTransition(() => {
      flushSync(() => {
        applyTheme();
      });
    });

    if (typeof transition?.finished?.finally === 'function') {
      transition.finished.finally(cleanup).catch(() => {});
    } else {
      cleanup();
    }

    const ready = transition?.ready;
    if (ready && typeof ready.then === 'function') {
      ready
        .then(() => {
          document.documentElement.animate(
            {
              clipPath,
            },
            {
              duration,
              easing: 'ease-in-out',
              fill: 'forwards',
              pseudoElement: '::view-transition-new(root)',
            }
          );
        })
        .catch(() => {});
    }
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error('useTheme must be used within ThemeProvider');
  return context;
}
