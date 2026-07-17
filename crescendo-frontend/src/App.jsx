import { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import useAuthStore from './store/authStore';
import ProtectedRoute from './components/ProtectedRoute';
import ToastProvider from './components/ToastProvider';
import './App.css';

// Landing
import DotCanvas from './components/DotCanvas';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Features from './components/Features';
import ApiSection from './components/ApiSection';
import DocsSection from './components/DocsSection';
import Footer from './components/Footer';

// Auth
import AuthLayout from './pages/auth/AuthLayout';
import Login from './pages/auth/Login';
import Register from './pages/auth/Register';
import ResetPassword from './pages/auth/ResetPassword';
import RecoverPasskey from './pages/auth/RecoverPasskey';
import VerifyEmail from './pages/auth/VerifyEmail';
import RevokeSessionConfirm from './pages/auth/RevokeSessionConfirm';
import OAuthCallback from './pages/auth/OAuthCallback';
import OAuthComplete from './pages/OAuthComplete';
import MfaChallenge from './pages/auth/MfaChallenge';

// Dashboard
import DashboardLayout from './pages/dashboard/DashboardLayout';
import Dashboard from './pages/dashboard/Dashboard';
import Workflows from './pages/dashboard/Workflows';
import WorkflowCanvas from './pages/dashboard/WorkflowCanvas';
import SharedWorkflows from './pages/dashboard/SharedWorkflows';
import History from './pages/dashboard/History';
import RunDetail from './pages/dashboard/RunDetail';
import Connections from './pages/dashboard/Connections';
import AdminPage from './pages/dashboard/AdminPage';

// Settings
import Settings from './pages/settings/Settings';
import ProfileSettings from './pages/settings/ProfileSettings';
import SecuritySettings from './pages/settings/SecuritySettings';
import ConnectedAccounts from './pages/settings/ConnectedAccounts';
import OAuthAppsSettings from './pages/settings/OAuthAppsSettings';
import DeveloperAppsSettings from './pages/settings/DeveloperAppsSettings';
import OAuthAuthorizePage from './pages/auth/OAuthAuthorizePage';

// Legal
import TermsPage from './pages/legal/TermsPage';
import PrivacyPage from './pages/legal/PrivacyPage';
import DeveloperProfile from './pages/DeveloperProfile';

// Email Service (dashboard feature)
import EmailService from './pages/dashboard/EmailService';
import ApiKeysSettings from './pages/settings/ApiKeysSettings';
import DomainsSettings from './pages/settings/DomainsSettings';
import TemplatesSettings from './pages/settings/TemplatesSettings';
import EmailLogs from './pages/settings/EmailLogs';
import ContactsPage from './pages/dashboard/email/ContactsPage';
import BroadcastsPage from './pages/dashboard/email/BroadcastsPage';
import EmailMetrics from './pages/dashboard/email/EmailMetrics';
import SuppressionsPage from './pages/dashboard/email/SuppressionsPage';

// Docs
import DocsPage from './pages/docs/DocsPage';

function LandingPage() {
  return (
    <>
      <DotCanvas />
      <div style={{ position: 'relative', zIndex: 1 }}>
        <Navbar />
        <Hero />
        <Features />
        <ApiSection />
        <DocsSection />
        <Footer />
      </div>
    </>
  );
}



function App() {
  const checkAuth = useAuthStore((state) => state.checkAuth);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  // Handle docs subdomain routing
  const isDocsSubdomain = window.location.hostname.startsWith('docs.');
  if (isDocsSubdomain) {
    return (
      <Routes>
        <Route path="/*" element={<DocsPage />} />
      </Routes>
    );
  }

  return (
    <>
    <ToastProvider />
    <Routes>
      {/* Landing */}
      <Route path="/" element={<LandingPage />} />
      <Route path="/developer" element={<DeveloperProfile />} />

      {/* Legal & Docs */}
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/docs/*" element={<DocsPage />} />

      {/* Auth */}
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/auth/recover-passkey" element={<RecoverPasskey />} />
        <Route path="/auth/revoke-session" element={<RevokeSessionConfirm />} />
      </Route>
      <Route path="/verify-email" element={<AuthLayout />}>
        <Route index element={<VerifyEmail />} />
      </Route>
      <Route path="/oauth/callback" element={<OAuthCallback />} />
      <Route path="/oauth-complete" element={<OAuthComplete />} />
      <Route path="/mfa/challenge" element={<AuthLayout />}>
        <Route index element={<MfaChallenge />} />
      </Route>

      {/* Dashboard (Protected) */}
      <Route element={<ProtectedRoute />}>
        <Route path="/oauth/authorize" element={<OAuthAuthorizePage />} />
        {/* Shared workflow import — inside ProtectedRoute but outside DashboardLayout */}
        <Route path="/shared" element={<DashboardLayout />}>
          <Route index element={<SharedWorkflows />} />
        </Route>
        <Route path="/shared/:shareId" element={<DashboardLayout />}>
          <Route index element={<SharedWorkflows />} />
        </Route>
        <Route path="/dashboard" element={<DashboardLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="workflows" element={<Workflows />} />
        <Route path="workflows/new" element={<WorkflowCanvas />} />
        <Route path="workflows/:workflowId" element={<WorkflowCanvas key={window.location.pathname} />} />
        <Route path="history" element={<History />} />
        <Route path="history/:workflowId/:runId" element={<RunDetail />} />
        <Route path="connections" element={<Connections />} />
        <Route path="admin" element={<AdminPage />} />

        {/* Email Service */}
        <Route path="email" element={<EmailService />}>
          <Route index element={<ApiKeysSettings />} />
          <Route path="domains" element={<DomainsSettings />} />
          <Route path="templates" element={<TemplatesSettings />} />
          <Route path="logs" element={<EmailLogs />} />
          <Route path="contacts" element={<ContactsPage />} />
          <Route path="broadcasts" element={<BroadcastsPage />} />
          <Route path="analytics" element={<EmailMetrics />} />
          <Route path="suppressions" element={<SuppressionsPage />} />
        </Route>

        {/* Settings */}
        <Route path="settings" element={<Settings />}>
          <Route index element={<ProfileSettings />} />
          <Route path="security" element={<SecuritySettings />} />
          <Route path="accounts" element={<ConnectedAccounts />} />
          <Route path="oauth-apps" element={<OAuthAppsSettings />} />
          <Route path="developer-api" element={<DeveloperAppsSettings />} />
        </Route>
      </Route>
      </Route>
    </Routes>
    </>
  );
}

export default App;
