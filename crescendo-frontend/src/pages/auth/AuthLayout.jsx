import { Outlet, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { HiSun, HiMoon } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import DotCanvas from '../../components/DotCanvas';
import './Auth.css';

export default function AuthLayout() {
    const { theme, toggleTheme } = useTheme();

    return (
        <div className="auth-split-layout">
            {/* Left Pane (Visuals / Branding) */}
            <div className="auth-split-left">
                <div className="auth-split-bg">
                    <DotCanvas />
                </div>
                
                <div className="auth-split-content">
                    <Link to="/" className="auth-brand">
                        <img src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'} alt="Crescendo" />
                        <span>Crescendo</span>
                    </Link>
                    
                    <div className="auth-brand-text">
                        <motion.h1 
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.1, duration: 0.8 }}
                        >
                            Automate workflows with precision
                        </motion.h1>
                        <motion.p
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.2, duration: 0.8 }}
                        >
                            Join thousands of developers building complex integrations, data pipelines, and intelligent automation in minutes.
                        </motion.p>
                    </div>

                    <div className="auth-brand-footer">
                        © 2026 Crescendo Inc.
                    </div>
                </div>
            </div>

            {/* Right Pane (Form) */}
            <div className="auth-split-right">
                <button className="auth-theme-toggle" onClick={toggleTheme}>
                    {theme === 'dark' ? <HiSun /> : <HiMoon />}
                </button>
                
                <div className="auth-page">
                    <motion.div
                        className="auth-form-container"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
                    >
                        <Outlet />
                    </motion.div>
                </div>
            </div>
        </div>
    );
}
