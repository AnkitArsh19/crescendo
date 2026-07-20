/* eslint-disable react-hooks/set-state-in-effect */
import { useState, useEffect } from 'react';
import { Outlet, NavLink, useLocation, Link, useNavigate } from 'react-router-dom';
import {
    HiOutlineViewGrid,
    HiOutlineCog,
    HiOutlineLightningBolt,
    HiOutlineClock,
    HiOutlineLink,
    HiOutlineLogout,
    HiOutlineUser,
    HiOutlineMail,
    HiMenuAlt2,
    HiOutlineBell,
    HiOutlineShieldCheck,
    HiSun,
    HiMoon,
} from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';
import { AnimatePresence, motion } from 'framer-motion';
import useAuthStore from '../../store/authStore';
import api from '../../api/axios';
import PasskeyNudge from '../../components/PasskeyNudge';
import useWorkflowEventStream from '../../hooks/useWorkflowEventStream';
import './DashboardLayout.css';

const navItems = [
    { to: '/dashboard', icon: <HiOutlineViewGrid />, label: 'Dashboard', end: true },
    { to: '/dashboard/workflows', icon: <HiOutlineLightningBolt />, label: 'Workflows' },
    { to: '/dashboard/history', icon: <HiOutlineClock />, label: 'History' },
    { to: '/dashboard/connections', icon: <HiOutlineLink />, label: 'Connections' },
];

export default function DashboardLayout() {
    useWorkflowEventStream();
    const { theme, toggleTheme } = useTheme();
    const location = useLocation();
    const navigate = useNavigate();
    const [dropdownOpen, setDropdownOpen] = useState(false);

    const { user, logout, isGuest, exitGuestMode } = useAuthStore();
    const [verifyBannerDismissed, setVerifyBannerDismissed] = useState(false);
    const [resending, setResending] = useState(false);

    // Auto-collapse sidebar on canvas pages (new workflow or editing existing)
    const isCanvas = location.pathname.includes('/workflows/new') ||
        /\/workflows\/[^/]+$/.test(location.pathname);
    const [collapsed, setCollapsed] = useState(isCanvas);

    useEffect(() => {
        setCollapsed(isCanvas);
    }, [isCanvas]);

    const getTitle = () => {
        const last = location.pathname.split('/').filter(Boolean).pop();
        if (last === 'dashboard') return 'Dashboard';
        if (last === 'new') return null; // Canvas manages its own title
        return last ? last.charAt(0).toUpperCase() + last.slice(1) : 'Dashboard';
    };

    const title = getTitle();

    return (
        <div className="dashboard">
            {/* Sidebar */}
            <aside className={`dash-sidebar ${collapsed ? 'collapsed' : ''} ${isCanvas ? 'canvas-mode' : ''}`}>
                <div className="dash-sidebar-header">
                    <img
                        src={theme === 'dark' ? '/logo-white.svg' : '/logo-black.svg'}
                        alt="Crescendo"
                        className="dash-sidebar-logo"
                    />
                    <span className="dash-sidebar-brand">Crescendo</span>
                </div>

                <nav className="dash-sidebar-nav">
                    <div className="dash-nav-section">Main</div>
                    {navItems.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.end}
                            className={({ isActive }) =>
                                `dash-nav-item ${isActive ? 'active' : ''}`
                            }
                        >
                            <span className="dash-nav-icon">{item.icon}</span>
                            <span className="dash-nav-label">{item.label}</span>
                        </NavLink>
                    ))}

                    {!isGuest && (
                        <>
                            <div className="dash-nav-section">Services</div>
                            <NavLink
                                to="/dashboard/email"
                                className={({ isActive }) =>
                                    `dash-nav-item ${isActive ? 'active' : ''}`
                                }
                            >
                                <span className="dash-nav-icon"><HiOutlineMail /></span>
                                <span className="dash-nav-label">Email</span>
                            </NavLink>

                            <div className="dash-nav-section">Account</div>
                            <NavLink
                                to="/dashboard/settings"
                                className={({ isActive }) =>
                                    `dash-nav-item ${isActive ? 'active' : ''}`
                                }
                            >
                                <span className="dash-nav-icon"><HiOutlineCog /></span>
                                <span className="dash-nav-label">Settings</span>
                            </NavLink>
                        </>
                    )}

                    {/* Admin section — only for admin users */}
                    {!isGuest && (user?.role === 'ADMIN' || user?.limits?.tier === 'ADMIN') && (
                        <>
                            <div className="dash-nav-section">Admin</div>
                            <NavLink
                                to="/dashboard/admin"
                                className={({ isActive }) =>
                                    `dash-nav-item ${isActive ? 'active' : ''}`
                                }
                            >
                                <span className="dash-nav-icon"><HiOutlineShieldCheck /></span>
                                <span className="dash-nav-label">Admin Panel</span>
                            </NavLink>
                        </>
                    )}
                </nav>

                {/* Plan / limits badge */}
                <div className="dash-plan-section">
                    {isGuest ? (
                        <div className="dash-plan-card dash-plan-guest">
                            <div className="dash-plan-label">🔒 Guest Mode</div>
                            <div className="dash-plan-limits">
                                <span>1 workflow</span>
                                <span>5 steps max</span>
                                <span>12hr trial</span>
                            </div>
                            <Link to="/register" onClick={() => exitGuestMode()} className="dash-plan-upgrade">Sign up →</Link>
                        </div>
                    ) : user?.limits?.limited && user?.hasLocalCredential ? (
                        <div className="dash-plan-card dash-plan-limited">
                            <div className="dash-plan-label">⚠ Unverified Account</div>
                            <div className="dash-plan-sub">Verify email to unlock full access</div>
                            <div className="dash-plan-limits">
                                <span>Workflows: {user.limits.maxWorkflows}</span>
                                <span>Connections: {user.limits.maxConnections}</span>
                            </div>
                        </div>
                    ) : user?.limits?.tier === 'ADMIN' ? (
                        <div className="dash-plan-card dash-plan-admin">
                            <div className="dash-plan-label">Admin</div>
                            <div className="dash-plan-sub">Unlimited access</div>
                        </div>
                    ) : user?.limits ? (
                        <div className="dash-plan-card dash-plan-standard">
                            <div className="dash-plan-label">Standard Plan</div>
                            <div className="dash-plan-limits">
                                <span>Workflows: up to {user.limits.maxWorkflows}</span>
                                <span>Connections: up to {user.limits.maxConnections}</span>
                            </div>
                        </div>
                    ) : null}
                </div>

                {/* User card + dropdown */}
                <div className="dash-sidebar-footer">
                    <div className="dash-user-dropdown">
                        <AnimatePresence>
                            {dropdownOpen && (
                                <motion.div
                                    className="dash-dropdown-menu"
                                    initial={{ opacity: 0, y: 6 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: 6 }}
                                    transition={{ duration: 0.15 }}
                                >
                                    <Link to="/dashboard/settings" className="dash-dropdown-item" onClick={() => setDropdownOpen(false)}>
                                        <span className="dash-nav-icon"><HiOutlineUser /></span>
                                        Profile
                                    </Link>
                                    <Link to="/dashboard/settings/security" className="dash-dropdown-item" onClick={() => setDropdownOpen(false)}>
                                        <span className="dash-nav-icon"><HiOutlineCog /></span>
                                        Settings
                                    </Link>
                                    <div className="dash-dropdown-divider" />
                                    <button 
                                        onClick={() => {
                                            setDropdownOpen(false);
                                            logout();
                                            navigate('/login');
                                        }} 
                                        className="dash-dropdown-item" 
                                        style={{ width: '100%', border: 'none', background: 'none', textAlign: 'left', fontFamily: 'inherit', fontSize: 'inherit' }}
                                    >
                                        <span className="dash-nav-icon"><HiOutlineLogout /></span>
                                        Log out
                                    </button>
                                </motion.div>
                            )}
                        </AnimatePresence>
                        <div
                            className="dash-user-card"
                            onClick={() => setDropdownOpen(!dropdownOpen)}
                        >
                            <div className="dash-user-avatar">{user?.username ? user.username.charAt(0).toUpperCase() : 'U'}</div>
                            <div className="dash-user-info">
                                <div className="dash-user-name">{user?.username || 'User'}</div>
                                <div className="dash-user-email">{user?.email || ''}</div>
                            </div>
                        </div>
                    </div>
                </div>
            </aside>

            {/* Main */}
            <div className={`dash-main ${isCanvas ? 'canvas-mode' : ''}`}>
                {/* Top bar — only show default topbar when NOT on canvas */}
                {!isCanvas && (
                    <div className="dash-topbar">
                        <div className="dash-topbar-left">
                            <span className="dash-topbar-title">{title}</span>
                        </div>
                        <div className="dash-topbar-right">
                            <button className="dash-topbar-btn" onClick={toggleTheme}>
                                {theme === 'dark' ? <HiSun /> : <HiMoon />}
                            </button>
                            <button className="dash-topbar-btn">
                                <HiOutlineBell />
                            </button>
                        </div>
                    </div>
                )}

                <div className={`dash-content ${isCanvas ? 'dash-content-canvas' : ''}`}>
                    {/* Guest Mode Banner */}
                    {isGuest && !isCanvas && (
                        <div className="dash-banner dash-banner-guest">
                            <span>🔒 Guest Mode — 1 workflow, 5 steps, 12hr trial.</span>
                            <Link to="/register" onClick={() => exitGuestMode()} className="dash-banner-cta">Sign up to unlock more</Link>
                        </div>
                    )}

                    {/* Email Verification Banner — only for LOCAL-credential users who haven't verified */}
                    {!isGuest && user?.hasLocalCredential && user?.limits?.limited && !verifyBannerDismissed && !isCanvas && (
                        <div className="dash-banner dash-banner-verify">
                            <HiOutlineMail />
                            <span>Verify your email to unlock all features.</span>
                            <button
                                className="dash-banner-cta"
                                disabled={resending}
                                onClick={async () => {
                                    setResending(true);
                                    try { await api.post('/auth/resend-verification'); } catch { /* */ }
                                    setResending(false);
                                }}
                            >
                                {resending ? 'Sending...' : 'Resend email'}
                            </button>
                            <button className="dash-banner-close" onClick={() => setVerifyBannerDismissed(true)}>✕</button>
                        </div>
                    )}

                    <PasskeyNudge />
                    <Outlet context={{ toggleTheme, theme, collapsed, setCollapsed }} />
                </div>
            </div>
        </div>
    );
}
