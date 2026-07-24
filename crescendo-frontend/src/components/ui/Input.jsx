import { useId } from 'react';
import './Input.css';

export default function Input({
    label,
    icon,
    rightIcon,
    onRightIconClick,
    error,
    className = '',
    id,
    ...props
}) {
    const fallbackId = useId();
    const inputId = id || fallbackId;

    return (
        <div className="ui-input-wrapper">
            {label && <label htmlFor={inputId} className="ui-input-label">{label}</label>}
            <div style={{ position: 'relative' }}>
                {icon && <span className="ui-input-icon">{icon}</span>}
                <input
                    id={inputId}
                    name={props.name || inputId}
                    className={`ui-input ${icon ? 'has-icon' : ''} ${rightIcon ? 'has-right-icon' : ''} ${error ? 'error' : ''} ${className}`}
                    {...props}
                />
                {rightIcon && (
                    <button
                        type="button"
                        className="ui-input-right-icon"
                        onClick={onRightIconClick}
                        tabIndex={-1}
                    >
                        {rightIcon}
                    </button>
                )}
            </div>
            {error && <div className="ui-input-error">{error}</div>}
        </div>
    );
}
