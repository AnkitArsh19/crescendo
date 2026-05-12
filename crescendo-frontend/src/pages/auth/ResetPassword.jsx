import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { HiOutlineMail, HiCheck, HiArrowLeft } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import Input from '../../components/ui/Input';
import './Auth.css';

export default function ResetPassword() {
    const { theme } = useTheme();
    const [sent, setSent] = useState(false);

    return (
        <div className="auth-card">
            <div className="auth-logo">
                <img
                    src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                    alt="Crescendo"
                />
                <span className="auth-logo-text">Crescendo</span>
            </div>

            <AnimatePresence mode="wait">
                {!sent ? (
                    <motion.div
                        key="form"
                        initial={{ opacity: 0, x: -10 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: 10 }}
                        transition={{ duration: 0.3 }}
                    >
                        <div className="auth-header">
                            <h1 className="auth-title">Reset password</h1>
                            <p className="auth-subtitle">
                                Enter your email and we'll send you a link to reset your password
                            </p>
                        </div>

                        <form
                            className="auth-form"
                            onSubmit={(e) => {
                                e.preventDefault();
                                setSent(true);
                            }}
                        >
                            <Input
                                label="Email"
                                type="email"
                                placeholder="you@example.com"
                                icon={<HiOutlineMail />}
                            />

                            <button type="submit" className="auth-btn">Send Reset Link</button>
                        </form>

                        <div className="auth-footer">
                            <Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                                <HiArrowLeft /> Back to login
                            </Link>
                        </div>
                    </motion.div>
                ) : (
                    <motion.div
                        key="success"
                        initial={{ opacity: 0, x: 10 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: -10 }}
                        transition={{ duration: 0.3 }}
                    >
                        <div className="auth-success">
                            <div className="auth-success-icon">
                                <HiCheck />
                            </div>
                            <h2 className="auth-title">Check your email</h2>
                            <p className="auth-subtitle" style={{ marginTop: 8 }}>
                                We've sent a password reset link to your email address. Please check
                                your inbox and follow the instructions.
                            </p>
                        </div>

                        <div className="auth-footer" style={{ marginTop: 24 }}>
                            <Link to="/login" style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                                <HiArrowLeft /> Back to login
                            </Link>
                        </div>
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    );
}
