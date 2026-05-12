import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlineSun, HiOutlineMoon, HiOutlineMenu, HiOutlineX } from 'react-icons/hi';
import { useTheme } from './ThemeContext';
import './Navbar.css';

export default function Navbar() {
    const { theme, toggleTheme } = useTheme();
    const [scrolled, setScrolled] = useState(false);
    const [mobileOpen, setMobileOpen] = useState(false);

    useEffect(() => {
        const onScroll = () => setScrolled(window.scrollY > 20);
        window.addEventListener('scroll', onScroll);
        return () => window.removeEventListener('scroll', onScroll);
    }, []);

    const links = [
        { label: 'Features', href: '#features' },
        { label: 'API', href: '#api' },
        { label: 'Docs', href: '#docs' },
    ];

    const logoSrc = theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg';

    return (
        <motion.nav
            className={`navbar ${scrolled ? 'scrolled' : ''}`}
            initial={{ y: -80, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
        >
            <div className="navbar-inner">
                <a href="#" className="navbar-logo">
                    <img src={logoSrc} alt="Crescendo" className="navbar-logo-img" />
                    <span className="navbar-logo-text">Crescendo</span>
                </a>

                <div className="navbar-center">
                    <ul className="navbar-links">
                        {links.map((link) => (
                            <li key={link.label}>
                                <a href={link.href} className="navbar-link">
                                    {link.label}
                                </a>
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="navbar-actions">
                    <button className="theme-toggle" onClick={toggleTheme} aria-label="Toggle theme">
                        <AnimatePresence mode="wait">
                            {theme === 'dark' ? (
                                <motion.span
                                    key="sun"
                                    initial={{ rotate: -90, opacity: 0, scale: 0.5 }}
                                    animate={{ rotate: 0, opacity: 1, scale: 1 }}
                                    exit={{ rotate: 90, opacity: 0, scale: 0.5 }}
                                    transition={{ duration: 0.25 }}
                                    style={{ display: 'flex' }}
                                >
                                    <HiOutlineSun />
                                </motion.span>
                            ) : (
                                <motion.span
                                    key="moon"
                                    initial={{ rotate: 90, opacity: 0, scale: 0.5 }}
                                    animate={{ rotate: 0, opacity: 1, scale: 1 }}
                                    exit={{ rotate: -90, opacity: 0, scale: 0.5 }}
                                    transition={{ duration: 0.25 }}
                                    style={{ display: 'flex' }}
                                >
                                    <HiOutlineMoon />
                                </motion.span>
                            )}
                        </AnimatePresence>
                    </button>
                    <Link to="/login" className="btn-login">Log in</Link>
                    <Link to="/register" className="btn-signup">Sign up</Link>
                    <button
                        className="navbar-mobile-toggle"
                        onClick={() => setMobileOpen(!mobileOpen)}
                    >
                        {mobileOpen ? <HiOutlineX /> : <HiOutlineMenu />}
                    </button>
                </div>
            </div>

            <AnimatePresence>
                {mobileOpen && (
                    <motion.div
                        className="navbar-mobile-menu"
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        exit={{ opacity: 0, y: -10 }}
                        transition={{ duration: 0.2 }}
                    >
                        {links.map((link) => (
                            <a
                                key={link.label}
                                href={link.href}
                                className="navbar-link"
                                onClick={() => setMobileOpen(false)}
                            >
                                {link.label}
                            </a>
                        ))}
                        <Link to="/login" className="btn-login" style={{ textAlign: 'left', padding: '10px 0', textDecoration: 'none' }} onClick={() => setMobileOpen(false)}>
                            Log in
                        </Link>
                    </motion.div>
                )}
            </AnimatePresence>
        </motion.nav>
    );
}
