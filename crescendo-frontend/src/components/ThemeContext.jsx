import { createContext, useContext, useState, useEffect, useCallback, useRef } from 'react';

const ThemeContext = createContext();

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => {
    const saved = localStorage.getItem('crescendo-theme');
    return saved || 'dark';
  });
  const isTransitioningRef = useRef(false);
  const lastToggleTimeRef = useRef(0);

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('crescendo-theme', theme);
  }, [theme]);

  const toggleTheme = useCallback((arg) => {
    const now = Date.now();
    // Protect against rapid toggling crashes with 400ms cooldown
    if (now - lastToggleTimeRef.current < 400 || isTransitioningRef.current) {
      return;
    }
    lastToggleTimeRef.current = now;

    const nextTheme = theme === 'dark' ? 'light' : 'dark';

    const applyTheme = () => {
      setTheme(nextTheme);
    };

    const prefersReducedMotion =
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-reduced-motion: reduce)')?.matches;

    if (typeof document.startViewTransition !== 'function' || prefersReducedMotion) {
      applyTheme();
      return;
    }

    const duration = 650;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let cx = null;
    let cy = null;

    // 1. Prioritize measuring the anchor object placed directly inside the theme button
    const anchor = document.getElementById('theme-toggle-anchor');
    if (anchor) {
      const rect = anchor.getBoundingClientRect();
      if (rect && (rect.top > 0 || rect.left > 0)) {
        cx = rect.left;
        cy = rect.top;
      }
    }

    // 2. Fallback to event target or bounding rect if anchor is not rendered
    if (cx == null || cy == null || (cx === 0 && cy === 0)) {
      if (arg && arg.currentTarget && typeof arg.currentTarget.getBoundingClientRect === 'function') {
        const rect = arg.currentTarget.getBoundingClientRect();
        cx = rect.left + rect.width / 2;
        cy = rect.top + rect.height / 2;
      } else if (arg && typeof arg.x === 'number' && typeof arg.y === 'number' && (arg.x > 0 || arg.y > 0)) {
        cx = arg.x;
        cy = arg.y;
      } else {
        const btn = document.querySelector('.crescendo-theme-toggle, .theme-toggle, .dash-topbar-btn');
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

    // Convert coordinates to percentages to handle DPI/display scaling cleanly
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

    try {
      const transition = document.startViewTransition(() => {
        applyTheme();
      });

      if (transition?.finished) {
        transition.finished.catch(() => {}).finally(cleanup);
      } else {
        cleanup();
      }

      if (transition?.ready) {
        transition.ready
          .then(() => {
            try {
              document.documentElement.animate(
                { clipPath },
                {
                  duration,
                  easing: 'ease-in-out',
                  fill: 'forwards',
                  pseudoElement: '::view-transition-new(root)',
                }
              );
            } catch {
              // ignore animation failure
            }
          })
          .catch(() => {});
      }

      // Safety timeout to ensure isTransitioningRef is ALWAYS released
      setTimeout(cleanup, duration + 100);
    } catch {
      applyTheme();
      cleanup();
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
