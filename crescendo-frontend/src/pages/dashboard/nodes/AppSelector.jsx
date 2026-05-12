import { useState, useMemo } from 'react';
import {
    HiOutlineLightningBolt,
    HiOutlineMail,
    HiOutlineDatabase,
    HiOutlineBell,
    HiOutlineCloud,
    HiOutlineCode,
    HiOutlineGlobe,
    HiOutlineClock,
    HiOutlineFilter,
    HiOutlineDocumentText,
} from 'react-icons/hi';
import { SiSlack, SiGmail, SiGithub, SiPostgresql, SiDiscord } from 'react-icons/si';

const categories = [
    {
        name: 'Communication',
        items: [
            { id: 'slack', name: 'Slack', desc: 'Send messages, alerts', icon: <SiSlack /> },
            { id: 'gmail', name: 'Gmail', desc: 'Send & read emails', icon: <SiGmail /> },
            { id: 'discord', name: 'Discord', desc: 'Post to channels', icon: <SiDiscord /> },
        ],
    },
    {
        name: 'Database',
        items: [
            { id: 'postgres', name: 'PostgreSQL', desc: 'Query & mutate data', icon: <SiPostgresql /> },
            { id: 'http', name: 'HTTP Request', desc: 'Call any REST API', icon: <HiOutlineGlobe /> },
        ],
    },
    {
        name: 'Cloud & Utils',
        items: [
            { id: 'github', name: 'GitHub', desc: 'Repos, issues, PRs', icon: <SiGithub /> },
            { id: 'webhook', name: 'Webhook', desc: 'Inbound HTTP trigger', icon: <HiOutlineLightningBolt /> },
            { id: 'schedule', name: 'Schedule', desc: 'Cron-based triggers', icon: <HiOutlineClock /> },
        ],
    },
    {
        name: 'Logic',
        items: [
            { id: 'condition', name: 'Condition', desc: 'If/else branching', icon: <HiOutlineFilter /> },
            { id: 'delay', name: 'Delay', desc: 'Wait before next step', icon: <HiOutlineClock /> },
            { id: 'transform', name: 'Transform', desc: 'Map & reshape data', icon: <HiOutlineCode /> },
        ],
    },
];

export default function AppSelector({ onAdd }) {
    const [search, setSearch] = useState('');

    const filtered = useMemo(() => {
        if (!search) return categories;
        const q = search.toLowerCase();
        return categories
            .map((cat) => ({
                ...cat,
                items: cat.items.filter(
                    (item) =>
                        item.name.toLowerCase().includes(q) ||
                        item.desc.toLowerCase().includes(q)
                ),
            }))
            .filter((cat) => cat.items.length > 0);
    }, [search]);

    const handleDragStart = (e, item) => {
        e.dataTransfer.setData('application/reactflow', JSON.stringify(item));
        e.dataTransfer.effectAllowed = 'move';
    };

    return (
        <div className="app-selector">
            <div className="app-selector-header">
                <div className="app-selector-title">Add Node</div>
                <input
                    type="text"
                    className="app-selector-search"
                    placeholder="Search apps…"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>
            <div className="app-selector-list">
                {filtered.map((cat) => (
                    <div key={cat.name}>
                        <div className="app-selector-category">{cat.name}</div>
                        {cat.items.map((item) => (
                            <div
                                key={item.id}
                                className="app-selector-item"
                                draggable
                                onDragStart={(e) => handleDragStart(e, item)}
                                onClick={() => onAdd?.(item)}
                            >
                                <div className="app-selector-item-icon">{item.icon}</div>
                                <div>
                                    <div className="app-selector-item-name">{item.name}</div>
                                    <div className="app-selector-item-desc">{item.desc}</div>
                                </div>
                            </div>
                        ))}
                    </div>
                ))}
            </div>
        </div>
    );
}
