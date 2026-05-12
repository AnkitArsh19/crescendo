import { NavLink, Outlet, useLocation } from 'react-router-dom';
import { HiOutlineKey, HiOutlineGlobe, HiOutlineTemplate, HiOutlineMail, HiOutlineUserGroup, HiOutlineSpeakerphone, HiOutlineChartBar, HiOutlineBan } from 'react-icons/hi';
import { motion } from 'framer-motion';
import './EmailService.css';

const tabs = [
    { to: '/dashboard/email', label: 'API Keys', icon: <HiOutlineKey />, end: true },
    { to: '/dashboard/email/domains', label: 'Domains', icon: <HiOutlineGlobe /> },
    { to: '/dashboard/email/templates', label: 'Templates', icon: <HiOutlineTemplate /> },
    { to: '/dashboard/email/logs', label: 'Email Logs', icon: <HiOutlineMail /> },
    { to: '/dashboard/email/contacts', label: 'Contacts', icon: <HiOutlineUserGroup /> },
    { to: '/dashboard/email/broadcasts', label: 'Broadcasts', icon: <HiOutlineSpeakerphone /> },
    { to: '/dashboard/email/analytics', label: 'Analytics', icon: <HiOutlineChartBar /> },
    { to: '/dashboard/email/suppressions', label: 'Suppressions', icon: <HiOutlineBan /> },
];

export default function EmailService() {
    const location = useLocation();

    return (
        <div className="email-service">
            <div className="email-service-header">
                <h1 className="email-service-title">Email Service</h1>
                <p className="email-service-desc">Send transactional emails, manage domains, templates, and API keys.</p>
            </div>

            <nav className="email-service-tabs">
                {tabs.map((tab) => (
                    <NavLink
                        key={tab.to}
                        to={tab.to}
                        end={tab.end}
                        className={({ isActive }) => `email-tab ${isActive ? 'active' : ''}`}
                    >
                        <span className="email-tab-icon">{tab.icon}</span>
                        {tab.label}
                    </NavLink>
                ))}
            </nav>

            <motion.div
                className="email-service-content"
                key={location.pathname}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
            >
                <Outlet />
            </motion.div>
        </div>
    );
}
