import { useState, useEffect, useMemo } from 'react';
import { HiPlus, HiTrash, HiCode } from 'react-icons/hi';
import { VariableInsertButton } from '../ConfigPanelBody';

const OPERATORS = [
    { value: 'equals', label: 'Equals' },
    { value: 'notEquals', label: 'Does not equal' },
    { value: 'contains', label: 'Contains' },
    { value: 'notContains', label: 'Does not contain' },
    { value: 'startsWith', label: 'Starts with' },
    { value: 'endsWith', label: 'Ends with' },
    { value: 'isEmpty', label: 'Is empty' },
    { value: 'isNotEmpty', label: 'Is not empty' },
    { value: 'greaterThan', label: 'Greater than (>)' },
    { value: 'lessThan', label: 'Less than (<)' },
    { value: 'greaterThanOrEqual', label: 'Greater than or equal (>=)' },
    { value: 'lessThanOrEqual', label: 'Less than or equal (<=)' },
    { value: 'regex', label: 'Matches Regex' },
];

export default function ConditionRuleBuilder({ mode = 'if', value, numberOutputs = 4, onChange, availableVariables = [] }) {
    const [showRawJson, setShowRawJson] = useState(false);
    const [rawJsonText, setRawJsonText] = useState('');
    const [jsonError, setJsonError] = useState(null);

    // Normalize incoming value to array structure
    const parsedData = useMemo(() => {
        if (!value) return mode === 'if' ? [{ combinator: 'AND', conditions: [] }] : [];
        if (typeof value === 'string') {
            try { return JSON.parse(value); }
            catch { return mode === 'if' ? [{ combinator: 'AND', conditions: [] }] : []; }
        }
        return value;
    }, [value, mode]);

    useEffect(() => {
        setRawJsonText(JSON.stringify(parsedData, null, 2));
    }, [parsedData]);

    const handleJsonTextChange = (text) => {
        setRawJsonText(text);
        try {
            const parsed = JSON.parse(text);
            setJsonError(null);
            onChange(parsed);
        } catch {
            setJsonError('Invalid JSON syntax');
        }
    };

    // ── IF MODE HANDLERS ───────────────────────────────────────────────────────

    const groups = Array.isArray(parsedData) && mode === 'if' ? parsedData : [{ combinator: 'AND', conditions: [] }];

    const addIfGroup = () => {
        const updated = [...groups, { combinator: 'AND', conditions: [{ leftValue: '', operator: 'equals', rightValue: '' }] }];
        onChange(updated);
    };

    const removeIfGroup = (groupIndex) => {
        const updated = groups.filter((_, idx) => idx !== groupIndex);
        onChange(updated.length > 0 ? updated : [{ combinator: 'AND', conditions: [] }]);
    };

    const updateGroupCombinator = (groupIndex, combinator) => {
        const updated = groups.map((g, idx) => idx === groupIndex ? { ...g, combinator } : g);
        onChange(updated);
    };

    const addIfCondition = (groupIndex) => {
        const updated = groups.map((g, idx) => {
            if (idx !== groupIndex) return g;
            const conds = Array.isArray(g.conditions) ? g.conditions : [];
            return {
                ...g,
                conditions: [...conds, { leftValue: '', operator: 'equals', rightValue: '' }]
            };
        });
        onChange(updated);
    };

    const updateIfCondition = (groupIndex, condIndex, field, newVal) => {
        const updated = groups.map((g, idx) => {
            if (idx !== groupIndex) return g;
            const conds = Array.isArray(g.conditions) ? g.conditions : [];
            const newConds = conds.map((c, cIdx) => cIdx === condIndex ? { ...c, [field]: newVal } : c);
            return { ...g, conditions: newConds };
        });
        onChange(updated);
    };

    const removeIfCondition = (groupIndex, condIndex) => {
        const updated = groups.map((g, idx) => {
            if (idx !== groupIndex) return g;
            const conds = Array.isArray(g.conditions) ? g.conditions : [];
            return { ...g, conditions: conds.filter((_, cIdx) => cIdx !== condIndex) };
        });
        onChange(updated);
    };

    // ── SWITCH MODE HANDLERS ───────────────────────────────────────────────────

    const rules = Array.isArray(parsedData) && mode === 'switch' ? parsedData : [];

    const addSwitchRule = () => {
        const nextOutputIndex = rules.length < numberOutputs ? rules.length : 0;
        const updated = [...rules, { value: '', operator: 'equals', matchValue: '', outputIndex: nextOutputIndex }];
        onChange(updated);
    };

    const updateSwitchRule = (ruleIndex, field, newVal) => {
        const updated = rules.map((r, idx) => idx === ruleIndex ? { ...r, [field]: newVal } : r);
        onChange(updated);
    };

    const removeSwitchRule = (ruleIndex) => {
        const updated = rules.filter((_, idx) => idx !== ruleIndex);
        onChange(updated);
    };

    return (
        <div className="cr-builder">
            <div className="cr-builder__header">
                <span className="cr-builder__title">
                    {mode === 'if' ? 'Condition Groups' : 'Routing Rules'}
                </span>
                <button
                    type="button"
                    className={`cr-builder__toggle-json ${showRawJson ? 'cr-builder__toggle-json--active' : ''}`}
                    onClick={() => setShowRawJson(!showRawJson)}
                    title="Toggle Raw JSON Editor"
                >
                    <HiCode /> {showRawJson ? 'Visual Builder' : 'Edit as JSON'}
                </button>
            </div>

            {showRawJson ? (
                <div className="cr-builder__raw-container">
                    <textarea
                        className="cr-builder__json-area"
                        value={rawJsonText}
                        onChange={(e) => handleJsonTextChange(e.target.value)}
                        rows={8}
                    />
                    {jsonError && <span className="cr-builder__json-error">{jsonError}</span>}
                </div>
            ) : mode === 'if' ? (
                /* ── IF MODE VISUAL BUILDER ── */
                <div className="cr-builder__groups">
                    {groups.map((group, gIdx) => {
                        const conds = Array.isArray(group.conditions) ? group.conditions : [];
                        return (
                            <div key={gIdx} className="cr-builder__group">
                                <div className="cr-builder__group-header">
                                    <span className="cr-builder__group-title">Group {gIdx + 1}</span>
                                    <div className="cr-builder__combinator-toggle">
                                        <button
                                            type="button"
                                            className={`cr-builder__comb-btn ${group.combinator === 'AND' ? 'active' : ''}`}
                                            onClick={() => updateGroupCombinator(gIdx, 'AND')}
                                        >
                                            AND
                                        </button>
                                        <button
                                            type="button"
                                            className={`cr-builder__comb-btn ${group.combinator === 'OR' ? 'active' : ''}`}
                                            onClick={() => updateGroupCombinator(gIdx, 'OR')}
                                        >
                                            OR
                                        </button>
                                    </div>
                                    {groups.length > 1 && (
                                        <button
                                            type="button"
                                            className="cr-builder__btn-danger-sm"
                                            onClick={() => removeIfGroup(gIdx)}
                                            title="Delete Group"
                                        >
                                            <HiTrash />
                                        </button>
                                    )}
                                </div>

                                <div className="cr-builder__rows">
                                    {conds.length === 0 ? (
                                        <span className="cr-builder__empty">No conditions in this group.</span>
                                    ) : (
                                        conds.map((cond, cIdx) => (
                                            <div key={cIdx} className="cr-builder__row">
                                                <div className="cr-builder__input-wrap">
                                                    <input
                                                        type="text"
                                                        className="cr-builder__input"
                                                        placeholder="Left value (e.g. {{steps.X.field}})"
                                                        value={cond.leftValue || ''}
                                                        onChange={(e) => updateIfCondition(gIdx, cIdx, 'leftValue', e.target.value)}
                                                    />
                                                    {availableVariables.length > 0 && (
                                                        <VariableInsertButton
                                                            availableVariables={availableVariables}
                                                            onInsert={(v) => updateIfCondition(gIdx, cIdx, 'leftValue', (cond.leftValue || '') + v)}
                                                        />
                                                    )}
                                                </div>

                                                <select
                                                    className="cr-builder__select"
                                                    value={cond.operator || 'equals'}
                                                    onChange={(e) => updateIfCondition(gIdx, cIdx, 'operator', e.target.value)}
                                                >
                                                    {OPERATORS.map((op) => (
                                                        <option key={op.value} value={op.value}>{op.label}</option>
                                                    ))}
                                                </select>

                                                {cond.operator !== 'isEmpty' && cond.operator !== 'isNotEmpty' && (
                                                    <input
                                                        type="text"
                                                        className="cr-builder__input"
                                                        placeholder="Right value"
                                                        value={cond.rightValue || ''}
                                                        onChange={(e) => updateIfCondition(gIdx, cIdx, 'rightValue', e.target.value)}
                                                    />
                                                )}

                                                <button
                                                    type="button"
                                                    className="cr-builder__icon-btn"
                                                    onClick={() => removeIfCondition(gIdx, cIdx)}
                                                    title="Remove condition"
                                                >
                                                    <HiTrash />
                                                </button>
                                            </div>
                                        ))
                                    )}
                                </div>

                                <button
                                    type="button"
                                    className="cr-builder__add-btn"
                                    onClick={() => addIfCondition(gIdx)}
                                >
                                    <HiPlus /> Add Condition
                                </button>
                            </div>
                        );
                    })}

                    <button
                        type="button"
                        className="cr-builder__add-group-btn"
                        onClick={addIfGroup}
                    >
                        <HiPlus /> Add Condition Group (OR)
                    </button>
                </div>
            ) : (
                /* ── SWITCH MODE VISUAL BUILDER ── */
                <div className="cr-builder__rules">
                    {rules.length === 0 ? (
                        <span className="cr-builder__empty">No routing rules added yet.</span>
                    ) : (
                        rules.map((rule, rIdx) => (
                            <div key={rIdx} className="cr-builder__row">
                                <div className="cr-builder__input-wrap">
                                    <input
                                        type="text"
                                        className="cr-builder__input"
                                        placeholder="Value (e.g. {{steps.X.status}})"
                                        value={rule.value || ''}
                                        onChange={(e) => updateSwitchRule(rIdx, 'value', e.target.value)}
                                    />
                                    {availableVariables.length > 0 && (
                                        <VariableInsertButton
                                            availableVariables={availableVariables}
                                            onInsert={(v) => updateSwitchRule(rIdx, 'value', (rule.value || '') + v)}
                                        />
                                    )}
                                </div>

                                <select
                                    className="cr-builder__select"
                                    value={rule.operator || 'equals'}
                                    onChange={(e) => updateSwitchRule(rIdx, 'operator', e.target.value)}
                                >
                                    {OPERATORS.map((op) => (
                                        <option key={op.value} value={op.value}>{op.label}</option>
                                    ))}
                                </select>

                                {rule.operator !== 'isEmpty' && rule.operator !== 'isNotEmpty' && (
                                    <input
                                        type="text"
                                        className="cr-builder__input"
                                        placeholder="Match value"
                                        value={rule.matchValue || ''}
                                        onChange={(e) => updateSwitchRule(rIdx, 'matchValue', e.target.value)}
                                    />
                                )}

                                <span className="cr-builder__arrow">&rarr;</span>

                                <select
                                    className="cr-builder__select cr-builder__select--output"
                                    value={rule.outputIndex ?? 0}
                                    onChange={(e) => updateSwitchRule(rIdx, 'outputIndex', parseInt(e.target.value, 10))}
                                >
                                    {Array.from({ length: numberOutputs }, (_, i) => (
                                        <option key={i} value={i}>Output {i + 1}</option>
                                    ))}
                                </select>

                                <button
                                    type="button"
                                    className="cr-builder__icon-btn"
                                    onClick={() => removeSwitchRule(rIdx)}
                                    title="Remove rule"
                                >
                                    <HiTrash />
                                </button>
                            </div>
                        ))
                    )}

                    <button
                        type="button"
                        className="cr-builder__add-btn"
                        onClick={addSwitchRule}
                    >
                        <HiPlus /> Add Routing Rule
                    </button>
                </div>
            )}
        </div>
    );
}
