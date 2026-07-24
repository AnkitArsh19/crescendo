import { useState, useEffect, useMemo } from 'react';
import { motion } from 'framer-motion';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';
import { HiOutlineChartBar } from 'react-icons/hi';
import { metricsApi } from '../../../api/metricsApi';
import '../../settings/Settings.css';
import './EmailMetrics.css';

const periods = [
    { label: '7 days', value: 7 },
    { label: '14 days', value: 14 },
    { label: '30 days', value: 30 },
    { label: '90 days', value: 90 },
];

const MONO_COLORS = [
    'var(--text-primary)',
    'var(--text-secondary)',
    'var(--border-color)',
    'var(--bg-card-hover)'
];

const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
        return (
            <div className="recharts-custom-tooltip">
                <p className="recharts-tooltip-label">{label}</p>
                {payload.map((entry, index) => (
                    <div key={index} className="recharts-tooltip-item">
                        <span className="recharts-tooltip-name">{entry.name}</span>
                        <span className="recharts-tooltip-value">{entry.value.toLocaleString()}</span>
                    </div>
                ))}
            </div>
        );
    }
    return null;
};

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
        
        return Object.values(byDate).sort((a, b) => a.date.localeCompare(b.date)).map(d => {
            const dateLabel = new Date(d.date + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
            return {
                ...d,
                displayDate: dateLabel,
                Total: (d.SENT || 0) + (d.DELIVERED || 0) + (d.FAILED || 0) + (d.BOUNCED || 0)
            };
        });
    }, [daily]);

    const breakdownData = useMemo(() => {
        return [
            { name: 'Delivered', value: summary.delivered || 0 },
            { name: 'Sent', value: summary.sent || 0 },
            { name: 'Pending', value: summary.pending || 0 },
            { name: 'Failed', value: summary.failed || 0 },
            { name: 'Bounced', value: summary.bounced || 0 },
            { name: 'Suppressed', value: summary.suppressed || 0 },
        ].filter(s => s.value > 0);
    }, [summary]);

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

    if (!metrics || Object.keys(metrics).length === 0) {
        return (
            <div className="settings-empty">
                <div className="settings-empty-icon"><HiOutlineChartBar size={32} /></div>
                <p>No email analytics available yet. Start sending emails to see your performance metrics here.</p>
            </div>
        );
    }

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ duration: 0.4 }}>
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

            <div className="metrics-charts-grid">
                {/* Daily Volume Area Chart */}
                {chartData.length > 0 && (
                    <div className="metrics-chart-section full-width">
                        <h3 className="metrics-chart-title">Daily Volume</h3>
                        <div className="recharts-wrapper">
                            <ResponsiveContainer width="100%" height={300}>
                                <AreaChart data={chartData} margin={{ top: 10, right: 0, left: 0, bottom: 0 }}>
                                    <defs>
                                        <linearGradient id="colorTotal" x1="0" y1="0" x2="0" y2="1">
                                            <stop offset="5%" stopColor="var(--text-primary)" stopOpacity={0.1}/>
                                            <stop offset="95%" stopColor="var(--text-primary)" stopOpacity={0}/>
                                        </linearGradient>
                                    </defs>
                                    <XAxis dataKey="displayDate" stroke="var(--border-color)" tick={{fill: 'var(--text-secondary)', fontSize: 12}} dy={10} axisLine={false} tickLine={false} />
                                    <YAxis stroke="var(--border-color)" tick={{fill: 'var(--text-secondary)', fontSize: 12}} dx={-10} axisLine={false} tickLine={false} />
                                    <Tooltip content={<CustomTooltip />} />
                                    <Area 
                                        type="monotone" 
                                        dataKey="Total" 
                                        stroke="var(--text-primary)" 
                                        strokeWidth={2}
                                        fillOpacity={1} 
                                        fill="url(#colorTotal)" 
                                        animationDuration={1500}
                                    />
                                </AreaChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                )}

                {/* Status Breakdown Donut Chart */}
                {breakdownData.length > 0 && (
                    <div className="metrics-chart-section">
                        <h3 className="metrics-chart-title">Status Breakdown</h3>
                        <div className="recharts-wrapper">
                            <ResponsiveContainer width="100%" height={260}>
                                <PieChart>
                                    <Pie
                                        data={breakdownData}
                                        cx="50%"
                                        cy="50%"
                                        innerRadius={60}
                                        outerRadius={90}
                                        paddingAngle={2}
                                        dataKey="value"
                                        stroke="var(--bg-card)"
                                        strokeWidth={2}
                                        animationDuration={1500}
                                    >
                                        {breakdownData.map((entry, index) => (
                                            <Cell key={`cell-${index}`} fill={MONO_COLORS[index % MONO_COLORS.length]} />
                                        ))}
                                    </Pie>
                                    <Tooltip content={<CustomTooltip />} />
                                    <Legend verticalAlign="bottom" height={36} wrapperStyle={{ fontSize: '12px', color: 'var(--text-secondary)' }} />
                                </PieChart>
                            </ResponsiveContainer>
                        </div>
                    </div>
                )}
            </div>
        </motion.div>
    );
}
