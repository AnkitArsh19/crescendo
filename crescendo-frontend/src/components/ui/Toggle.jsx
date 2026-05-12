import './Toggle.css';

export default function Toggle({ checked, onChange, label }) {
    return (
        <label className="ui-toggle">
            <div
                className={`ui-toggle-track ${checked ? 'active' : ''}`}
                onClick={() => onChange?.(!checked)}
            >
                <div className="ui-toggle-thumb" />
            </div>
            {label && <span className="ui-toggle-label">{label}</span>}
        </label>
    );
}
