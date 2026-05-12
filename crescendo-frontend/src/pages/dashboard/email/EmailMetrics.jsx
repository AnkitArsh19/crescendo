import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import { metricsApi } from '../../../api/metricsApi';
import '../../settings/Settings.css';

const periods = [
    { label: '7 days', value: 7 },
    { label: '14 days', value: 14 },
    { label: '30 days', value: 30 },
    { label: '90 days', value: 90 },
];

export default function EmailMetrics() {
    const [metrics, setMetrics] = useState(null);
    const [days, setDays] = useState(30);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        (async () => {
            setLoading(true);
            try {
                const data = await metricsApi.get(days);
                setMetrics(data);
            } catch { /* */ }
            setLoading(false);
        })();
    }, [days]);

    const summary = metrics?.summary || {};
    const daily = metrics?.daily || [];

    // Build chart data from daily entries
    const chartData = useMemo(() => {
        const byDate = {};
        daily.forEach(({ date, status, count }) => {
            if (!byDate[date]) byDate[date] = { date };
            byDate[date][status] = (byDate[date][status] || 0) + count;
        });
        return Object.values(byDate).sort((a, b) => a.date.localeCompare(b.date));
    }, [daily]);

    // Find max for bar chart scaling
    const maxDaily = useMemo(() => {
        return Math.max(1, ...chartData.map(d => {
            const total = (d.SENT || 0) + (d.DELIVERED || 0) + (d.FAILED || 0) + (d.BOUNCED || 0);
            return total;
        }));
    }, [chartData]);

    const statCards = [
        { label: 'Total', value: summary.total || 0 },
        { label: 'Delivered', value: summary.delivered || 0 },
        { label: 'Opens', value: summary.totalOpens || 0 },
        { label: 'Clicks', value: summary.totalClicks || 0 },
        { label: 'Failed', value: summary.failed || 0 },
        { label: 'Bounced', value: summary.bounced || 0 },
    ];

    if (loading) {
        return (
            <div className="settings-skeleton-list">
                {[...Array(3)].map((_, i) => <div key={i} className="settings-skeleton-row" style={{ height: 80 }} />)}
            </div>
        );
    }

    if (!metrics) {
        return (
            <div className="settings-empty">
                <div className="settings-empty-icon">📊</div>
                <p>No email metrics available yet. Send some emails to see analytics.</p>
            </div>
        );
    }

    return (
        <>
            {/* Period selector */}
            <div className="settings-section-header">
                <div>
                    <h2 className="settings-section-title">Email Analytics</h2>
                    <p className="settings-section-desc">Track your email sending performance.</p>
                </div>
                <div className="metrics-period-select">
                    {periods.map(p => (
                        <button
                            key={p.value}
                            className={`metrics-period-btn ${days === p.value ? 'active' : ''}`}
                            onClick={() => setDays(p.value)}
                        >
                            {p.label}
                        </button>
                    ))}
                </div>
            </div>

            {/* Stat Cards */}
            <div className="metrics-cards">
                {statCards.map((s, i) => (
                    <motion.div
                        key={s.label}
                        className="metrics-card"
                        initial={{ opacity: 0, y: 8 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: i * 0.04 }}
                    >
                        <div className="metrics-card-value">{s.value.toLocaleString()}</div>
                        <div className="metrics-card-label">{s.label}</div>
                    </motion.div>
                ))}
            </div>

            {/* Daily Volume Bar Chart */}
            {chartData.length > 0 && (
                <div className="metrics-chart-section">
                    <h3 className="metrics-chart-title">Daily Volume</h3>
                    <div className="metrics-bar-chart">
                        {chartData.map((d, i) => {
                            const total = (d.SENT || 0) + (d.DELIVERED || 0) + (d.FAILED || 0) + (d.BOUNCED || 0);
                            const pct = (total / maxDaily) * 100;
                            const dateLabel = new Date(d.date + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
                            return (
                                <div key={i} className="metrics-bar-col" title={`${dateLabel}: ${total} emails`}>
                                    <div className="metrics-bar-track">
                                        <motion.div
                                            className="metrics-bar-fill"
                                            initial={{ height: 0 }}
                                            animate={{ height: `${pct}%` }}
                                            transition={{ delay: i * 0.02, duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
                                        />
                                    </div>
                                    <span className="metrics-bar-label">{dateLabel}</span>
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* Status Breakdown */}
            <div className="metrics-chart-section">
                <h3 className="metrics-chart-title">Status Breakdown</h3>
                <div className="metrics-breakdown">
                    {[
                        { label: 'Delivered', value: summary.delivered || 0 },
                        { label: 'Sent', value: summary.sent || 0 },
                        { label: 'Pending', value: summary.pending || 0 },
                        { label: 'Failed', value: summary.failed || 0 },
                        { label: 'Bounced', value: summary.bounced || 0 },
                        { label: 'Suppressed', value: summary.suppressed || 0 },
                    ].filter(s => s.value > 0).map((s) => {
                        const pct = summary.total > 0 ? Math.round((s.value / summary.total) * 100) : 0;
                        return (
                            <div key={s.label} className="metrics-breakdown-row">
                                <span className="metrics-breakdown-label">{s.label}</span>
                                <div className="metrics-breakdown-bar-track">
                                    <motion.div
                                        className="metrics-breakdown-bar"
                                        initial={{ width: 0 }}
                                        animate={{ width: `${pct}%` }}
                                        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                                    />
                                </div>
                                <span className="metrics-breakdown-value">{s.value.toLocaleString()} ({pct}%)</span>
                            </div>
                        );
                    })}
                </div>
            </div>
        </>
    );
}
