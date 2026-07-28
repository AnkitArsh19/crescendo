import React from 'react';
import './AnimatedCircularProgressBar.css';

export function AnimatedCircularProgressBar({
    max = 100,
    min = 0,
    value = 0,
    gaugePrimaryColor = "var(--text-primary, #ffffff)",
    gaugeSecondaryColor = "var(--border-secondary, rgba(255, 255, 255, 0.12))",
    size = 54,
    strokeWidth = 9,
    className = "",
}) {
    const radius = 50 - strokeWidth / 2;
    const circumference = 2 * Math.PI * radius;
    const boundedValue = Math.min(max, Math.max(min, value));
    const percent = Math.round(((boundedValue - min) / (max - min)) * 100);
    const strokeDashoffset = circumference - (percent / 100) * circumference;

    return (
        <div 
            className={`circular-progress-container ${className}`.trim()} 
            style={{ width: size, height: size }}
        >
            <svg className="circular-progress-svg" viewBox="0 0 100 100">
                <circle
                    cx="50"
                    cy="50"
                    r={radius}
                    strokeWidth={strokeWidth}
                    strokeDashoffset="0"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="circular-progress-bg-circle"
                    style={{ stroke: gaugeSecondaryColor }}
                    fill="transparent"
                />
                <circle
                    cx="50"
                    cy="50"
                    r={radius}
                    strokeWidth={strokeWidth}
                    strokeDasharray={`${circumference} ${circumference}`}
                    strokeDashoffset={strokeDashoffset}
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="circular-progress-bar-circle"
                    style={{ stroke: gaugePrimaryColor }}
                    fill="transparent"
                />
            </svg>
            <span className="circular-progress-label">
                {percent}%
            </span>
        </div>
    );
}
export default AnimatedCircularProgressBar;
