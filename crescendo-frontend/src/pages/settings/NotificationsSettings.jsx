import React, { useState, useEffect } from 'react';
import api from '../../api/axios';
import {
  getNotificationPreferences,
  updateNotificationPreference,
  getWorkflowNotificationSetting,
  updateWorkflowNotificationSetting,
} from '../../api/notifications';
import './NotificationsSettings.css';

export default function NotificationsSettings() {
  const [preferences, setPreferences] = useState([]);
  const [loading, setLoading] = useState(true);

  // Workflow settings state
  const [workflows, setWorkflows] = useState([]);
  const [wfSearch, setWfSearch] = useState('');
  const [workflowSettings, setWorkflowSettings] = useState({});

  useEffect(() => {
    loadPreferences();
    loadWorkflows();
  }, []);

  const loadPreferences = async () => {
    try {
      const data = await getNotificationPreferences();
      setPreferences(data || []);
    } catch (err) {
      console.error('Failed to load notification preferences:', err);
    } finally {
      setLoading(false);
    }
  };

  const loadWorkflows = async () => {
    try {
      const res = await api.get('/workflows');
      const list = Array.isArray(res.data) ? res.data : res.data?.content || [];
      setWorkflows(list);

      const settingsMap = {};
      await Promise.all(
        list.map(async (wf) => {
          try {
            const s = await getWorkflowNotificationSetting(wf.id);
            if (s?.notifyMode) {
              settingsMap[wf.id] = s.notifyMode;
            }
          } catch {
            settingsMap[wf.id] = 'ALWAYS';
          }
        })
      );
      setWorkflowSettings(settingsMap);
    } catch (err) {
      console.error('Failed to load workflows:', err);
    }
  };

  const handleToggle = async (pref) => {
    const nextEnabled = !pref.enabled;

    // Optimistic update
    setPreferences((prev) =>
      prev.map((p) =>
        p.type === pref.type ? { ...p, enabled: nextEnabled } : p
      )
    );

    try {
      await updateNotificationPreference(pref.type, {
        enabled: nextEnabled,
      });
    } catch (err) {
      console.error('Failed to update preference:', err);
      loadPreferences();
    }
  };

  const handleWorkflowModeChange = async (workflowId, mode) => {
    setWorkflowSettings((prev) => ({ ...prev, [workflowId]: mode }));
    try {
      await updateWorkflowNotificationSetting(workflowId, mode);
    } catch (err) {
      console.error('Failed to update workflow setting:', err);
    }
  };

  // Group preferences by category
  const categories = preferences.reduce((acc, pref) => {
    const cat = pref.category || 'General';
    if (!acc[cat]) acc[cat] = [];
    acc[cat].push(pref);
    return acc;
  }, {});

  const filteredWorkflows = workflows.filter((w) =>
    (w.name || '').toLowerCase().includes(wfSearch.toLowerCase())
  );

  return (
    <div className="notif-settings-container">
      {/* Header */}
      <div>
        <h2 className="settings-section-title">Notification Settings</h2>
        <p className="settings-section-desc">
          Customize which event types appear in your in-app inbox and configure per-workflow run alerts.
        </p>
      </div>

      {/* Preferences by Category */}
      <div>
        <h3 className="settings-section-title" style={{ fontSize: '1rem', marginBottom: '16px' }}>
          Event Preferences
        </h3>

        {loading ? (
          <p style={{ color: 'var(--text-secondary)' }}>Loading preferences...</p>
        ) : (
          Object.entries(categories).map(([category, items]) => (
            <div key={category} className="notif-category-card">
              <div className="notif-category-header">
                <h4 className="notif-category-title">{category}</h4>
                <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                  Inbox Alert
                </span>
              </div>

              <div>
                {items.map((pref) => (
                  <div key={pref.type} className="notif-pref-row">
                    <span className="notif-pref-name">{pref.displayName}</span>

                    <label className="notif-switch" title="Toggle notification">
                      <input
                        type="checkbox"
                        checked={pref.enabled}
                        onChange={() => handleToggle(pref)}
                      />
                      <span className="notif-slider" />
                    </label>
                  </div>
                ))}
              </div>
            </div>
          ))
        )}
      </div>

      {/* Per-Workflow Notifications */}
      {workflows.length > 0 && (
        <div>
          <h3 className="settings-section-title" style={{ fontSize: '1rem', marginBottom: '4px' }}>
            Per-Workflow Run Alerts
          </h3>
          <p className="settings-section-desc">
            Adjust notification frequency for specific workflows. Ideal for silencing high-frequency workflows.
          </p>

          <input
            type="text"
            className="notif-wf-search"
            placeholder="Search workflows..."
            value={wfSearch}
            onChange={(e) => setWfSearch(e.target.value)}
          />

          <div className="notif-category-card">
            <div className="notif-category-header">
              <h4 className="notif-category-title">Workflow</h4>
              <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
                Notification Mode
              </span>
            </div>

            <div>
              {filteredWorkflows.map((wf) => {
                const currentMode = workflowSettings[wf.id] || 'ALWAYS';
                return (
                  <div key={wf.id} className="notif-wf-row">
                    <div className="notif-wf-info">
                      <span className="notif-wf-name">{wf.name}</span>
                      {wf.description && (
                        <span className="notif-wf-desc">{wf.description}</span>
                      )}
                    </div>

                    <select
                      className="notif-wf-select"
                      value={currentMode}
                      onChange={(e) => handleWorkflowModeChange(wf.id, e.target.value)}
                    >
                      <option value="ALWAYS">All Runs (Success & Failure)</option>
                      <option value="FAILURE_ONLY">Failures Only</option>
                      <option value="NEVER">Never Notify</option>
                    </select>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
