import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { HiOutlineUser, HiOutlineLockClosed, HiOutlineLink, HiOutlineKey, HiOutlineCode } from 'react-icons/hi';
import { motion } from 'framer-motion';
import './Settings.css';

const tabs = [
    { to: '/dashboard/settings', label: 'Profile', icon: <HiOutlineUser />, end: true },
    { to: '/dashboard/settings/security', label: 'Security', icon: <HiOutlineLockClosed /> },
    { to: '/dashboard/settings/accounts', label: 'Connected Accounts', icon: <HiOutlineLink /> },
    { to: '/dashboard/settings/oauth-apps', label: 'OAuth Apps', icon: <HiOutlineKey /> },
    { to: '/dashboard/settings/developer-api', label: 'Developer API', icon: <HiOutlineCode /> },
];

export default function Settings() {
    const location = useLocation();

    return (
        <div className="settings">
            <nav className="settings-nav">
                {tabs.map((tab) => (
                    <NavLink
                        key={tab.to}
                        to={tab.to}
                        end={tab.end}
                        className={({ isActive }) =>
                            `settings-nav-item ${isActive ? 'active' : ''}`
                        }
                    >
                        <span className="settings-nav-icon">{tab.icon}</span>
                        {tab.label}
                    </NavLink>
                ))}
            </nav>

            <motion.div
                className="settings-content"
                key={location.pathname}
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
            >
                <Outlet />
            </motion.div>
        </div>
    );
}

