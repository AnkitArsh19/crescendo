import React, { useState, useMemo } from 'react';
import {
    HiClock,
    HiCalendar,
    HiLightningBolt,
    HiChevronLeft,
    HiChevronRight,
    HiInformationCircle,
} from 'react-icons/hi';
import { VariableInsertButton } from '../ConfigPanelBody';

const MONTH_NAMES = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
];

const DAYS_OF_WEEK = ['Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa', 'Su'];

/**
 * Custom Dark-Themed Calendar & Time Picker for Crescendo.
 * Zero browser-default popovers, zero emojis, seamless theme integration.
 */
function CustomCalendarTimePicker({ value, onChange }) {
    // Parse current value or default to today
    const parsedDate = useMemo(() => {
        if (!value || value.startsWith('{{')) return new Date();
        const d = new Date(value);
        return Number.isNaN(d.getTime()) ? new Date() : d;
    }, [value]);

    const [currentYear, setCurrentYear] = useState(parsedDate.getFullYear());
    const [currentMonth, setCurrentMonth] = useState(parsedDate.getMonth());

    const selectedYear = parsedDate.getFullYear();
    const selectedMonth = parsedDate.getMonth();
    const selectedDate = parsedDate.getDate();

    const selectedHours = String(parsedDate.getHours()).padStart(2, '0');
    const selectedMinutes = String(parsedDate.getMinutes()).padStart(2, '0');

    // Navigation
    const handlePrevMonth = () => {
        if (currentMonth === 0) {
            setCurrentMonth(11);
            setCurrentYear(y => y - 1);
        } else {
            setCurrentMonth(m => m - 1);
        }
    };

    const handleNextMonth = () => {
        if (currentMonth === 11) {
            setCurrentMonth(0);
            setCurrentYear(y => y + 1);
        } else {
            setCurrentMonth(m => m + 1);
        }
    };

    // Build calendar grid days
    const calendarDays = useMemo(() => {
        const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
        // Monday is index 0 in our DAYS_OF_WEEK
        const startingDay = (firstDayOfMonth.getDay() + 6) % 7;
        const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
        const daysInPrevMonth = new Date(currentYear, currentMonth, 0).getDate();

        const days = [];

        // Previous month padding
        for (let i = startingDay - 1; i >= 0; i--) {
            days.push({
                day: daysInPrevMonth - i,
                isCurrentMonth: false,
                year: currentMonth === 0 ? currentYear - 1 : currentYear,
                month: currentMonth === 0 ? 11 : currentMonth - 1,
            });
        }

        // Current month days
        for (let i = 1; i <= daysInMonth; i++) {
            days.push({
                day: i,
                isCurrentMonth: true,
                year: currentYear,
                month: currentMonth,
            });
        }

        // Next month padding to fill complete 5-6 week rows
        const remaining = (7 - (days.length % 7)) % 7;
        for (let i = 1; i <= remaining; i++) {
            days.push({
                day: i,
                isCurrentMonth: false,
                year: currentMonth === 11 ? currentYear + 1 : currentYear,
                month: currentMonth === 11 ? 0 : currentMonth + 1,
            });
        }

        return days;
    }, [currentYear, currentMonth]);

    const handleSelectDay = (cell) => {
        const newD = new Date(parsedDate);
        newD.setFullYear(cell.year);
        newD.setMonth(cell.month);
        newD.setDate(cell.day);
        onChange(newD.toISOString());
    };

    const handleTimeChange = (hours, minutes) => {
        const newD = new Date(parsedDate);
        newD.setHours(Number(hours));
        newD.setMinutes(Number(minutes));
        newD.setSeconds(0);
        onChange(newD.toISOString());
    };

    const isToday = (cell) => {
        const today = new Date();
        return today.getFullYear() === cell.year &&
               today.getMonth() === cell.month &&
               today.getDate() === cell.day;
    };

    const isSelected = (cell) => {
        return value && !value.startsWith('{{') &&
               selectedYear === cell.year &&
               selectedMonth === cell.month &&
               selectedDate === cell.day;
    };

    const hoursOptions = Array.from({ length: 24 }, (_, i) => String(i).padStart(2, '0'));
    const minutesOptions = ['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'];

    return (
        <div className="custom-dt-picker">
            {/* Header: Month & Year Controls */}
            <div className="custom-dt-header">
                <button type="button" className="custom-dt-nav-btn" onClick={handlePrevMonth} title="Previous Month">
                    <HiChevronLeft />
                </button>
                <div className="custom-dt-month-title">
                    <span>{MONTH_NAMES[currentMonth]}</span>
                    <span>{currentYear}</span>
                </div>
                <button type="button" className="custom-dt-nav-btn" onClick={handleNextMonth} title="Next Month">
                    <HiChevronRight />
                </button>
            </div>

            {/* Days of Week */}
            <div className="custom-dt-weekdays">
                {DAYS_OF_WEEK.map(d => (
                    <div key={d} className="custom-dt-weekday">{d}</div>
                ))}
            </div>

            {/* Calendar Days Grid */}
            <div className="custom-dt-grid">
                {calendarDays.map((cell, idx) => {
                    const selected = isSelected(cell);
                    const today = isToday(cell);
                    return (
                        <button
                            key={idx}
                            type="button"
                            className={`custom-dt-day ${cell.isCurrentMonth ? '' : 'outside'} ${selected ? 'selected' : ''} ${today ? 'today' : ''}`}
                            onClick={() => handleSelectDay(cell)}
                        >
                            <span>{cell.day}</span>
                        </button>
                    );
                })}
            </div>

            {/* Time Selector Bar */}
            <div className="custom-dt-time-row">
                <div className="custom-dt-time-label">
                    <HiClock />
                    <span>Time</span>
                </div>
                <div className="custom-dt-time-inputs">
                    <select
                        className="custom-dt-select"
                        value={selectedHours}
                        onChange={(e) => handleTimeChange(e.target.value, selectedMinutes)}
                    >
                        {hoursOptions.map(h => (
                            <option key={h} value={h}>{h} hr</option>
                        ))}
                    </select>
                    <span className="custom-dt-colon">:</span>
                    <select
                        className="custom-dt-select"
                        value={selectedMinutes}
                        onChange={(e) => handleTimeChange(selectedHours, e.target.value)}
                    >
                        {minutesOptions.map(m => (
                            <option key={m} value={m}>{m} min</option>
                        ))}
                    </select>
                </div>
                <button
                    type="button"
                    className="custom-dt-now-btn"
                    onClick={() => onChange(new Date().toISOString())}
                >
                    Now
                </button>
            </div>

            {/* Selected Value Output */}
            {value && !value.startsWith('{{') && (
                <div className="custom-dt-preview">
                    <span className="custom-dt-preview-label">Selected:</span>
                    <code>{value}</code>
                </div>
            )}
        </div>
    );
}

/**
 * DateTimePickerField — rich visual selector for Date, Time and Dynamic Execution Timestamps.
 * Clean, modern, emoji-free styling matching Crescendo's design tokens.
 */
export function DateTimePickerField({
    field,
    value,
    onChange,
    availableVariables,
}) {
    const [mode, setMode] = useState(() => {
        if (!value) return 'dynamic';
        if (value.startsWith('{{now') || value === '{{today}}' || value.startsWith('{{$now')) return 'dynamic';
        if (value.startsWith('{{steps.')) return 'variable';
        return 'picker';
    });

    const dynamicPresets = [
        { label: 'Execution Time (Now)', value: '{{now}}', desc: 'When workflow runs' },
        { label: 'Now + 2 mins', value: '{{now + 2m}}', desc: '2 minutes after run' },
        { label: 'Now + 5 mins', value: '{{now + 5m}}', desc: '5 minutes after run' },
        { label: 'Now + 15 mins', value: '{{now + 15m}}', desc: '15 minutes after run' },
        { label: 'Now + 1 hour', value: '{{now + 1h}}', desc: '1 hour after run' },
        { label: 'Tomorrow', value: '{{now + 1d}}', desc: '24 hours after run' },
        { label: 'Today (Date Only)', value: '{{today}}', desc: 'YYYY-MM-DD' },
    ];

    React.useEffect(() => {
        if (!value && field?.required) {
            onChange?.(field?.default || '{{now}}');
        }
    }, []);

    return (
        <div className="dt-picker-container">
            {/* Mode Switcher Tabs */}
            <div className="dt-picker-tabs">
                <button
                    type="button"
                    className={`dt-picker-tab ${mode === 'dynamic' ? 'active' : ''}`}
                    onClick={() => setMode('dynamic')}
                >
                    <HiLightningBolt />
                    <span>Dynamic Time</span>
                </button>
                <button
                    type="button"
                    className={`dt-picker-tab ${mode === 'picker' ? 'active' : ''}`}
                    onClick={() => setMode('picker')}
                >
                    <HiCalendar />
                    <span>Specific Date & Time</span>
                </button>
                <button
                    type="button"
                    className={`dt-picker-tab ${mode === 'custom' ? 'active' : ''}`}
                    onClick={() => setMode('custom')}
                >
                    <HiClock />
                    <span>Custom / Variable</span>
                </button>
            </div>

            {/* Mode 1: Dynamic Execution Presets */}
            {mode === 'dynamic' && (
                <div className="dt-picker-dynamic-box">
                    <div className="dt-picker-chips">
                        {dynamicPresets.map((preset) => {
                            const isSelected = value === preset.value;
                            return (
                                <button
                                    key={preset.value}
                                    type="button"
                                    className={`dt-chip ${isSelected ? 'selected' : ''}`}
                                    onClick={() => onChange(preset.value)}
                                    title={preset.desc}
                                >
                                    <span className="dt-chip-label">{preset.label}</span>
                                </button>
                            );
                        })}
                    </div>
                    <div className="dt-picker-dynamic-hint">
                        <HiInformationCircle />
                        <span>Dynamic timestamps calculate the exact execution time automatically whenever this workflow is triggered.</span>
                    </div>
                </div>
            )}

            {/* Mode 2: Custom Dark Calendar & Time Picker */}
            {mode === 'picker' && (
                <CustomCalendarTimePicker
                    value={value}
                    onChange={onChange}
                />
            )}

            {/* Mode 3: Custom / Variable Input */}
            {mode === 'custom' && (
                <div className="cpb-input-with-vars">
                    <input
                        className="cpb-input"
                        type="text"
                        value={value || ''}
                        placeholder={field.placeholder || '{{now}} or ISO datetime string'}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {availableVariables && availableVariables.length > 0 && (
                        <VariableInsertButton
                            availableVariables={availableVariables}
                            onInsert={(tpl) => onChange(tpl)}
                        />
                    )}
                </div>
            )}
        </div>
    );
}
