import { useState, useEffect, useCallback, useRef, useMemo } from 'react';
import { resourceApi } from '../../api/workflowApi';
import { connectionsApi } from '../../api/connectionsApi';
import { appCatalogApi } from '../../api/appCatalogApi';
import SearchableSelect from '../../components/ui/SearchableSelect';
import TestResultPanel from './TestResultPanel';
import useToastStore from '../../store/toastStore';
import useAuthStore from '../../store/authStore';
import { parseConfigSchema } from '../../workflow/workflowGraphSerializer';
import { HiCheck, HiPlus, HiLightningBolt, HiChevronRight, HiX, HiOutlinePencil, HiOutlineTrash, HiUpload } from 'react-icons/hi';
import { HiOutlineBolt } from 'react-icons/hi2';

// ─────────────────────────────────────────────────────────────────────────────
// Common output fields per app — used when we don't have real test data yet
// ─────────────────────────────────────────────────────────────────────────────

// Trigger output fields — what pollers actually return
const TRIGGER_OUTPUT_FIELDS = {
    'gmail': ['subject', 'fromEmail', 'fromName', 'snippet', 'id'],
    'microsoft-outlook': ['subject', 'fromEmail', 'fromName', 'bodyPreview', 'receivedDateTime', 'id'],
    'discord': ['content', 'author', 'channelId', 'guildId', 'timestamp', 'messageId'],
    'slack': ['text', 'user', 'channel', 'timestamp', 'threadTs'],
    'github': ['action', 'title', 'url', 'sender', 'repository'],
    'gitlab': ['action', 'title', 'url', 'author', 'project'],
    'google-calendar': ['eventId', 'summary', 'start', 'end', 'location'],
    'google-drive': ['fileId', 'fileName', 'mimeType', 'webViewLink'],
    'google-sheets': ['values', 'range', 'spreadsheetId'],
    'google-forms': ['responseId', 'answers', 'respondentEmail'],
    'google-slides': ['presentationId', 'title', 'slideCount'],
    'crescendo-webhook': ['body', 'headers', 'method', 'url'],
    'rss': ['title', 'link', 'description', 'pubDate'],
    'spotify': ['trackName', 'artistName', 'albumName', 'addedAt'],
    'linkedin': ['postId', 'author', 'content', 'timestamp'],
    'twitter': ['tweetId', 'text', 'author', 'createdAt'],
    'microsoft-teams': ['messageId', 'content', 'from', 'channelId'],
    'microsoft-excel': ['rowIndex', 'values', 'worksheetName'],
    'figma': ['fileKey', 'name', 'lastModified', 'version'],
    'strava': ['activityId', 'name', 'type', 'distance', 'movingTime'],
    'airtable': ['recordId', 'fields', 'tableName'],
    'notion': ['pageId', 'title', 'url'],
    'linear': ['issueId', 'title', 'state', 'assignee'],
    'toggl': ['entryId', 'description', 'duration', 'projectName'],
    'google-tasks': ['taskId', 'title', 'status', 'due'],
    '__default__': ['data', 'id', 'status', 'message'],
};

// Action output fields — what handlers actually return
const ACTION_OUTPUT_FIELDS = {
    'gmail': ['provider', 'to', 'subject', 'response'],
    'microsoft-outlook': ['provider', 'statusCode', 'sentTo', 'subject', 'response'],
    'discord': ['provider', 'channelId', 'response'],
    'slack': ['provider', 'channel', 'response'],
    'github': ['provider', 'action', 'owner', 'repo', 'response'],
    'gitlab': ['provider', 'action', 'projectId', 'response'],
    'google-calendar': ['provider', 'calendarId', 'response'],
    'google-sheets': ['provider', 'action', 'spreadsheetId', 'range', 'response'],
    'google-docs': ['provider', 'response'],
    'google-drive': ['provider', 'response'],
    'google-forms': ['provider', 'formId', 'response'],
    'google-slides': ['provider', 'presentationId', 'response'],
    'google-tasks': ['provider', 'taskId', 'title', 'response'],
    'microsoft-teams': ['provider', 'response'],
    'microsoft-excel': ['provider', 'response'],
    'openai': ['response', 'text', 'model', 'usage'],
    'gemini': ['response', 'text', 'model'],
    'http': ['response', 'statusCode', 'headers', 'body'],
    'crescendo-webhook': ['provider', 'url', 'response'],
    'crescendo-email': ['provider', 'to', 'subject', 'response'],
    'airtable': ['response', 'id', 'fields'],
    'notion': ['response', 'id', 'url', 'title'],
    'linear': ['response', 'issueId', 'title', 'state'],
    'toggl': ['response', 'entryId', 'description', 'duration'],
    'cat-facts': ['response', 'fact', 'length'],
    'giphy': ['response', 'url', 'title', 'resultCount'],
    'quotes': ['response', 'quote', 'author', 'category'],
    'joke-api': ['response', 'joke', 'setup', 'delivery', 'type'],
    'nasa-apod': ['response', 'title', 'url', 'explanation', 'date', 'photoCount'],
    'weather': ['response', 'city', 'temperature', 'description', 'forecastCount'],
    'linkedin': ['response', 'profile', 'id'],
    'twitter': ['response', 'tweetId', 'text'],
    'figma': ['response', 'fileKey', 'name'],
    'strava': ['response', 'activityId', 'name', 'type', 'distance'],
    'github-stats': ['response', 'username', 'repos', 'followers'],
    'leetcode': ['response', 'username', 'solved', 'query'],
    'pomodoro': ['startTime', 'endTime', 'durationMinutes', 'label', 'task'],
    'sarvam': ['response', 'translatedText', 'audios'],
    'log': ['message'],
    'job-search': ['jobs', 'totalFound', 'totalBeforeDedup', 'sources', 'query', 'location'],
    'spotify': ['response', 'tracks', 'artists', 'albums', 'playlists'],
    'telegram': ['response', 'messageId', 'chatId'],
    '__default__': ['response', 'status', 'data', 'message'],
};

function getOutputFieldsForApp(appKey, isTriggerStep) {
    if (isTriggerStep) {
        return TRIGGER_OUTPUT_FIELDS[appKey] || TRIGGER_OUTPUT_FIELDS['__default__'];
    }
    return ACTION_OUTPUT_FIELDS[appKey] || ACTION_OUTPUT_FIELDS['__default__'];
}

// ─────────────────────────────────────────────────────────────────────────────
// DynamicDropdownField — uses SearchableSelect + resourceApi
// ─────────────────────────────────────────────────────────────────────────────

function DynamicDropdownField({ field, appKey, connectionId, config, value, onChange }) {
    const [options, setOptions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const prevParamsRef = useRef('');

    const canFetch = appKey && connectionId && field.resourceType
        && field.dependsOn.every((dep) => config[dep]);

    const fetchOptions = useCallback(async () => {
        if (!canFetch) return;
        const params = {};
        field.dependsOn.forEach((dep) => { params[dep] = config[dep]; });

        const paramKey = JSON.stringify(params);
        if (paramKey === prevParamsRef.current) return;
        prevParamsRef.current = paramKey;

        setLoading(true);
        setError(null);
        try {
            const data = await resourceApi.list(appKey, field.resourceType, connectionId, params);
            setOptions((data || []).map((o) => ({
                id: o.id,
                label: o.label || o.id,
                description: o.description && o.description !== '0 items'
                    ? `${o.description} · ID: ${o.id}`
                    : `ID: ${o.id}`,
            })));
        } catch {
            setError('Failed to load options');
            setOptions([]);
        } finally {
            setLoading(false);
        }
    }, [canFetch, appKey, connectionId, field.resourceType, field.dependsOn, config]);

    useEffect(() => { fetchOptions(); }, [fetchOptions]);

    useEffect(() => {
        if (!canFetch) {
            setOptions([]);
            prevParamsRef.current = '';
        }
    }, [canFetch]);

    if (!canFetch && field.dependsOn.length > 0) {
        return (
            <SearchableSelect
                options={[]}
                value=""
                placeholder={`Select ${field.dependsOn.join(', ')} first…`}
                disabled
            />
        );
    }

    return (
        <SearchableSelect
            options={options}
            value={value || ''}
            onChange={onChange}
            placeholder={`Select ${field.label}…`}
            loading={loading}
            error={error}
            onRefresh={() => { prevParamsRef.current = ''; fetchOptions(); }}
            emptyMessage={`No ${field.label.toLowerCase()} found`}
        />
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// VariableInsertButton — dropdown to insert {{step.N.field}} references
// ─────────────────────────────────────────────────────────────────────────────

function VariableInsertButton({ availableVariables, onInsert }) {
    const [open, setOpen] = useState(false);
    const [search, setSearch] = useState('');
    const ref = useRef(null);

    // Close on outside click
    useEffect(() => {
        if (!open) return;
        const handler = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false); };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [open]);

    if (!availableVariables || availableVariables.length === 0) return null;

    const filtered = search.trim()
        ? availableVariables.map((group) => ({
            ...group,
            fields: group.fields.filter((f) => f.toLowerCase().includes(search.toLowerCase())),
        })).filter((g) => g.fields.length > 0)
        : availableVariables;

    return (
        <div className="var-insert" ref={ref}>
            <button
                type="button"
                className="var-insert__btn"
                title="Insert data from a previous step"
                onClick={() => setOpen(!open)}
            >
                <HiLightningBolt /> <span>Insert Data</span>
            </button>
            {open && (
                <div className="var-insert__dropdown">
                    <div className="var-insert__search">
                        <input
                            type="text"
                            placeholder="Search fields…"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            autoFocus
                        />
                    </div>
                    <div className="var-insert__list">
                        {filtered.map((group) => (
                            <div key={group.stepIndex} className="var-insert__group">
                                <div className="var-insert__group-header">
                                    <span className="var-insert__step-badge">{group.stepIndex}</span>
                                    <span>{group.appName}</span>
                                    <span className="var-insert__op-tag">{group.operationName}</span>
                                </div>
                                {group.fields.map((fieldName) => (
                                    <button
                                        key={fieldName}
                                        className="var-insert__field"
                                        onClick={() => {
                                            onInsert(`{{steps.${group.stepIndex}.${fieldName}}}`);
                                            setOpen(false);
                                            setSearch('');
                                        }}
                                    >
                                        <span className="var-insert__field-name">{fieldName}</span>
                                        <span className="var-insert__field-ref">steps.{group.stepIndex}.{fieldName}</span>
                                    </button>
                                ))}
                            </div>
                        ))}
                        {filtered.length === 0 && (
                            <div className="var-insert__empty">No matching fields</div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// ─────────────────────────────────────────────────────────────────────────────
// DynamicField — renders the correct input per field type
// ─────────────────────────────────────────────────────────────────────────────

function DynamicField({ field, appKey, connectionId, config, value, onChange, availableVariables }) {
    const inputRef = useRef(null);

    // Insert variable template at cursor position or append
    const handleInsertVariable = (template) => {
        const el = inputRef.current;
        if (el) {
            const start = el.selectionStart ?? (value || '').length;
            const end = el.selectionEnd ?? start;
            const current = value || '';
            const newVal = current.slice(0, start) + template + current.slice(end);
            onChange(newVal);
            // Restore cursor after the inserted template
            setTimeout(() => {
                el.focus();
                const pos = start + template.length;
                el.setSelectionRange(pos, pos);
            }, 0);
        } else {
            onChange((value || '') + template);
        }
    };

    const hasVars = availableVariables && availableVariables.length > 0;

    switch (field.type) {
        case 'dynamic_dropdown':
            return (
                <DynamicDropdownField
                    field={field} appKey={appKey} connectionId={connectionId}
                    config={config} value={value} onChange={onChange}
                />
            );

        case 'multi_select_tags': {
            const tags = Array.isArray(value) ? value : (value ? String(value).split(',').map(s => s.trim()).filter(Boolean) : []);
            const presetOptions = (field.options || []).map(o => typeof o === 'string' ? o : o.label || o.value);
            const [tagInput, setTagInput] = useState('');
            const [showSuggestions, setShowSuggestions] = useState(false);
            const suggestions = presetOptions.filter(o => !tags.includes(o) && o.toLowerCase().includes(tagInput.toLowerCase()));
            const addTag = (t) => { if (t && !tags.includes(t)) { onChange([...tags, t]); } setTagInput(''); setShowSuggestions(false); };
            const removeTag = (t) => onChange(tags.filter(x => x !== t));
            return (
                <div className="cpb-tags-container">
                    <div className="cpb-tags-wrap">
                        {tags.map(t => (
                            <span key={t} className="cpb-tag">{t}<button type="button" className="cpb-tag-x" onClick={() => removeTag(t)}><HiX /></button></span>
                        ))}
                        <input
                            className="cpb-tag-input"
                            value={tagInput}
                            placeholder={tags.length === 0 ? (field.placeholder || 'Type to add…') : 'Add more…'}
                            onChange={e => { setTagInput(e.target.value); setShowSuggestions(true); }}
                            onFocus={() => setShowSuggestions(true)}
                            onBlur={() => setTimeout(() => setShowSuggestions(false), 200)}
                            onKeyDown={e => {
                                if (e.key === 'Enter' && tagInput.trim()) { e.preventDefault(); addTag(tagInput.trim()); }
                                if (e.key === 'Backspace' && !tagInput && tags.length) removeTag(tags[tags.length - 1]);
                            }}
                        />
                    </div>
                    {showSuggestions && suggestions.length > 0 && (
                        <div className="cpb-tags-suggestions">
                            {suggestions.slice(0, 8).map(s => (
                                <button key={s} type="button" className="cpb-tags-suggestion" onMouseDown={() => addTag(s)}>{s}</button>
                            ))}
                        </div>
                    )}
                </div>
            );
        }

        case 'dropdown':
            return (
                <SearchableSelect
                    options={(field.options || []).map((opt) =>
                        typeof opt === 'string' ? { id: opt, label: opt } : { id: opt.value, label: opt.label }
                    )}
                    value={value || ''}
                    onChange={onChange}
                    placeholder={`Select ${field.label}…`}
                    searchable={field.options?.length > 6}
                />
            );

        case 'select':
            return (
                <select
                    className="cpb-input cpb-select"
                    value={value || ''}
                    onChange={(e) => onChange(e.target.value)}
                >
                    {(field.options || []).map((opt) => {
                        const optVal = typeof opt === 'string' ? opt : (opt.value ?? '');
                        const optLabel = typeof opt === 'string' ? opt : (opt.label || opt.value || '');
                        return (
                            <option key={optVal} value={optVal}>{optLabel}</option>
                        );
                    })}
                </select>
            );

        case 'textarea':
            return (
                <div className="cpb-input-with-vars">
                    <textarea
                        ref={inputRef}
                        className="cpb-input cpb-textarea"
                        value={value || ''}
                        placeholder={field.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'boolean':
            return (
                <label className="cpb-checkbox-label">
                    <input
                        type="checkbox"
                        checked={value === true || value === 'true'}
                        onChange={(e) => onChange(e.target.checked)}
                        className="cpb-checkbox"
                    />
                    <span>{field.label}</span>
                </label>
            );

        case 'number':
            return (
                <input
                    className="cpb-input" type="number"
                    value={value ?? ''} placeholder={field.placeholder}
                    onChange={(e) => onChange(e.target.value)}
                />
            );

        case 'json':
            return (
                <div className="cpb-input-with-vars">
                    <textarea
                        ref={inputRef}
                        className="cpb-input cpb-json"
                        value={typeof value === 'object' ? JSON.stringify(value, null, 2) : (value || '')}
                        placeholder={field.placeholder || '{}'}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'password':
            return (
                <input
                    className="cpb-input" type="password"
                    value={value || ''} placeholder={field.placeholder}
                    onChange={(e) => onChange(e.target.value)}
                />
            );

        case 'array':
            return (
                <div className="cpb-input-with-vars">
                    <input
                        ref={inputRef}
                        className="cpb-input"
                        value={Array.isArray(value) ? value.join(', ') : (value || '')}
                        placeholder={field.placeholder || 'value1, value2, value3'}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );

        case 'file': {
            const fileInputRef = useRef(null);
            const [fileName, setFileName] = useState(
                value && typeof value === 'object' ? value.name : (value ? 'File selected' : '')
            );
            const [isDragging, setIsDragging] = useState(false);
            const [isUploading, setIsUploading] = useState(false);
            const addToast = useToastStore(s => s.addToast);
            const { token } = useAuthStore();

            const handleFileChange = (e) => {
                const file = e.target.files?.[0];
                if (!file) return;
                processFile(file);
            };

            const processFile = async (file) => {
                const maxSizeMB = field.maxSizeMB || 25;
                if (file.size > maxSizeMB * 1024 * 1024) {
                    addToast(`File size exceeds the limit of ${maxSizeMB}MB`, 'error');
                    return;
                }

                setIsUploading(true);
                setFileName(file.name);

                try {
                    const formData = new FormData();
                    formData.append('file', file);
                    // Default to RELAY if not explicitly set to RETAINED
                    const consumptionModel = field.consumptionModel || 'RELAY';
                    formData.append('consumptionModel', consumptionModel);
                    if (field.maxSizeMB) formData.append('maxSizeMB', field.maxSizeMB);

                    const res = await fetch('/api/v1/files/upload', {
                        method: 'POST',
                        headers: { 'Authorization': `Bearer ${token}` },
                        body: formData
                    });

                    if (!res.ok) {
                        const errText = await res.text();
                        throw new Error(errText || 'Upload failed');
                    }

                    const data = await res.json();
                    onChange(data); // Emits structured reference: { name, contentType, sizeBytes, storageKey, checksum, consumptionModel }
                    addToast('File uploaded successfully', 'success');
                } catch (err) {
                    console.error('File upload error:', err);
                    addToast(`Failed to upload file: ${err.message}`, 'error');
                    setFileName('');
                } finally {
                    setIsUploading(false);
                }
            };

            const handleDragOver = (e) => {
                e.preventDefault();
                setIsDragging(true);
            };

            const handleDragLeave = (e) => {
                e.preventDefault();
                setIsDragging(false);
            };

            const handleDrop = (e) => {
                e.preventDefault();
                setIsDragging(false);
                const file = e.dataTransfer.files?.[0];
                if (file) {
                    processFile(file);
                }
            };

            const acceptStr = field.accept || '*/*';
            const maxSizeMB = field.maxSizeMB || 25;

            return (
                <div 
                    className={`cpb-file-upload ${isDragging ? 'dragging' : ''}`}
                    onDragOver={handleDragOver}
                    onDragLeave={handleDragLeave}
                    onDrop={handleDrop}
                >
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept={acceptStr}
                        className="cpb-file-hidden"
                        onChange={handleFileChange}
                        disabled={isUploading}
                    />
                    {isUploading ? (
                        <div className="cpb-file-uploading">
                            <span>Uploading...</span>
                        </div>
                    ) : fileName ? (
                        <div className="cpb-file-selected">
                            <div className="cpb-file-icon"><HiUpload /></div>
                            <div className="cpb-file-info">
                                <span className="cpb-file-name">{fileName}</span>
                                <span className="cpb-file-meta">Selected for upload</span>
                            </div>
                            <button
                                type="button"
                                className="cpb-file-remove"
                                onClick={(e) => { 
                                    e.stopPropagation();
                                    setFileName(''); 
                                    onChange(null); 
                                    if (fileInputRef.current) fileInputRef.current.value = ''; 
                                }}
                            >
                                <HiX />
                            </button>
                        </div>
                    ) : (
                        <div 
                            className="cpb-file-dropzone"
                            onClick={() => fileInputRef.current?.click()}
                        >
                            <div className="cpb-file-dropzone-icon">
                                <HiUpload />
                            </div>
                            <div className="cpb-file-dropzone-text">
                                <span>Click to upload</span> or drag and drop
                            </div>
                            <div className="cpb-file-dropzone-hint">
                                Max {maxSizeMB}MB · {acceptStr === '*/*' ? 'Any file type' : acceptStr}
                            </div>
                        </div>
                    )}
                </div>
            );
        }

        default: // text
            return (
                <div className="cpb-input-with-vars">
                    <input
                        ref={inputRef}
                        className="cpb-input" type="text"
                        value={value || ''} placeholder={field.placeholder}
                        onChange={(e) => onChange(e.target.value)}
                    />
                    {hasVars && <VariableInsertButton availableVariables={availableVariables} onInsert={handleInsertVariable} />}
                </div>
            );
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TABS
// ─────────────────────────────────────────────────────────────────────────────

const TABS = ['Setup', 'Configure', 'Test'];

// ─────────────────────────────────────────────────────────────────────────────
// ConfigPanelBody — 3-tab stepper
// ─────────────────────────────────────────────────────────────────────────────

export default function ConfigPanelBody({
    configNode, updateNodeData, connectedAppOptions, selectedConnections,
    selectedTriggerOptions, selectedActionOptions, ensureAppDetail, appDetailsByKey,
    catalogApps, allNodes, allConnections, onOpenAppBrowser,
    onClose, onDelete, onClear, onSaveAndClose, onNavigate, nodeCount, nodeIndex,
}) {
    const { data } = configNode;
    const isTrigger = configNode.type === 'trigger';
    const [activeTab, setActiveTab] = useState(0);
    const [editingName, setEditingName] = useState(false);
    const [stepName, setStepName] = useState('');
    const [accountMenuOpen, setAccountMenuOpen] = useState(false);
    const accountMenuRef = useRef(null);

    const user = useAuthStore(state => state.user);
    const isAdmin = user?.role === 'ADMIN';

    // ── Resolve configSchema ──
    const appDetail = appDetailsByKey?.[data.appKey];
    const actionOrTriggerKey = isTrigger ? (data.triggerKey || data.actionKey) : data.actionKey;

    const configSchema = useMemo(() => {
        if (!appDetail || !actionOrTriggerKey) return [];
        const listKey = isTrigger ? 'triggers' : 'actions';
        const keyField = isTrigger ? 'triggerKey' : 'actionKey';
        const items = appDetail[listKey] || [];
        const match = items.find((i) => i[keyField] === actionOrTriggerKey);
        return parseConfigSchema(match?.configSchema || {});
    }, [appDetail, actionOrTriggerKey, isTrigger]);

    // ── Detect if app needs auth (connection) ──
    const selectedCatalogApp = (catalogApps || []).find((a) => a.appKey === data.appKey);
    const isNoAuthApp = selectedCatalogApp?.authType === 'NONE';

    // ── Tab completion state ──
    const isUsingAdminKey = data.credentialSource === 'ADMIN_KEY' && appDetail?.hasPlatformKey;
    const hasConnection = isNoAuthApp || !!data.connectionId || isUsingAdminKey;
    const setupComplete = !!(data.appKey && hasConnection && actionOrTriggerKey);
    const configComplete = setupComplete && configSchema.every((f) => {
        if (!f.required) return true;
        const val = (data.configuration || {})[f.key];
        return val != null && String(val).trim() !== '';
    });

    // ── Previous step variables for data passing ──
    const previousStepVariables = useMemo(() => {
        if (!allNodes || !configNode) return [];
        const currentStepIndex = configNode.data?.stepIndex;
        // With 1-based indexing: trigger=1, first action=2
        // Only steps >= 2 can reference previous steps
        if (currentStepIndex == null || currentStepIndex < 2) return [];

        return allNodes
            .filter((n) => {
                const si = n.data?.stepIndex;
                return si != null && si < currentStepIndex && n.data?.appKey;
            })
            .sort((a, b) => (a.data.stepIndex || 0) - (b.data.stepIndex || 0))
            .map((n) => {
                const nd = n.data;
                const isTriggerStep = n.type === 'trigger';
                const appMeta = (catalogApps || []).find((a) => a.appKey === nd.appKey);
                const appName = appMeta?.name || nd.appName || nd.appKey;
                const opName = nd.triggerName || nd.actionName
                    || nd.triggerKey || nd.actionKey || (isTriggerStep ? 'Trigger' : 'Action');
                const fields = getOutputFieldsForApp(nd.appKey, isTriggerStep);
                return {
                    stepIndex: nd.stepIndex,
                    appKey: nd.appKey,
                    appName,
                    operationName: opName,
                    fields,
                };
            });
    }, [allNodes, configNode, catalogApps]);

    // ── Config update helper ──
    const updateConfig = (key, value) => {
        const newConfig = { ...(data.configuration || {}), [key]: value };
        configSchema.forEach((field) => {
            if (field.dependsOn.includes(key) && newConfig[field.key] !== undefined) {
                delete newConfig[field.key];
            }
        });
        updateNodeData(configNode.id, { configuration: newConfig });
    };

    // ── Connection options for SearchableSelect ──
    const connectionOptions = selectedConnections.map((c) => ({
        id: c.id,
        label: c.name || c.appKey,
        description: c.accountEmail || c.accountDisplayName || null,
    }));

    // ── App options — show ALL catalog apps, not just connected ones ──
    const appOptions = (catalogApps || []).map((a) => ({
        id: a.appKey,
        label: a.name || a.appKey,
        description: a.category || null,
        iconUrl: a.iconUrl || null,
    }));

    // Extract granted scopes from the active connection
    const activeConnection = selectedConnections.find((c) => c.id === data.connectionId);
    const grantedScopes = activeConnection?.grantedScopes ? activeConnection.grantedScopes.split(/[\s,]+/).filter(Boolean) : null;

    // ── Trigger/Action options ──
    const operationOptions = (isTrigger ? selectedTriggerOptions : selectedActionOptions).map((opt) => {
        const kf = isTrigger ? 'triggerKey' : 'actionKey';
        let isGreyedOut = false;
        let tooltip = null;

        if (opt.requiredScopes && grantedScopes) {
            const missingScopes = opt.requiredScopes.filter((s) => !grantedScopes.includes(s));
            if (missingScopes.length > 0) {
                isGreyedOut = true;
                tooltip = `Missing required scopes: ${missingScopes.join(', ')}. Please reconnect your account and grant these permissions.`;
            }
        }

        return {
            id: opt[kf] || opt.name || '',
            label: opt.name || opt[kf] || '',
            description: opt.description || null,
            disabled: isGreyedOut,
            tooltip: tooltip,
        };
    });

    // Handle app selection (including via browser modal)
    const handleAppSelect = async (appKey) => {
        const appMeta = (catalogApps || []).find((a) => a.appKey === appKey);
        updateNodeData(configNode.id, {
            appKey,
            app: appKey,
            appName: appMeta?.name || appKey,
            iconUrl: appMeta?.iconUrl || null,
            connectionId: null,
            actionKey: '',
            triggerKey: '',
            triggerType: '',
            triggerName: '',
            actionName: '',
            action: '',
            configuration: {},
        });
        await ensureAppDetail(appKey);
    };

    // Handle action/trigger selection
    const handleOperationSelect = (key) => {
        const optionsList = isTrigger ? selectedTriggerOptions : selectedActionOptions;
        const keyField = isTrigger ? 'triggerKey' : 'actionKey';
        const match = optionsList.find((o) => o[keyField] === key);
        const name = match?.name || key;
        const appMeta = (catalogApps || []).find((a) => a.appKey === data.appKey);
        const appName = appMeta?.name || data.appKey;

        if (isTrigger) {
            updateNodeData(configNode.id, {
                triggerKey: key, actionKey: key, triggerType: key,
                triggerName: name, label: `${appName} · ${name}`,
                configuration: {},
            });
        } else {
            updateNodeData(configNode.id, {
                actionKey: key, action: key, actionName: name,
                label: `${appName} · ${name}`, configuration: {},
            });
        }
    };

    // Handle OAuth connect inline — optionally pass connectionId for reconnection
    const handleNewConnection = async (reconnectConnectionId) => {
        if (!data.appKey) return;
        try {
            const { authorizationUrl } = await appCatalogApi.getOAuthUrl(data.appKey, reconnectConnectionId || undefined);
            if (authorizationUrl) window.open(authorizationUrl, '_blank', 'width=600,height=700');
        } catch {
            // non-fatal
        }
    };

    // Close account menu on outside click
    useEffect(() => {
        if (!accountMenuOpen) return;
        const handler = (e) => { if (accountMenuRef.current && !accountMenuRef.current.contains(e.target)) setAccountMenuOpen(false); };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, [accountMenuOpen]);

    // Derive step display name
    const displayStepName = data.stepLabel || data.triggerName || data.actionName || data.appName || (isTrigger ? 'Configure Trigger' : 'Configure Action');
    const stepNumber = (nodeIndex ?? -1) + 1;

    return (
        <div className="cpb-container">
            {/* ── Header (Zapier-style) ── */}
            <div className="canvas-config-header">
                <div className="canvas-config-header-left">
                    {data.iconUrl && (
                        <img src={data.iconUrl} alt="" className="canvas-config-header-icon" />
                    )}
                    {editingName ? (
                        <input
                            className="cpb-name-input"
                            autoFocus
                            value={stepName}
                            onChange={e => setStepName(e.target.value)}
                            onBlur={() => {
                                if (stepName.trim()) updateNodeData(configNode.id, { stepLabel: stepName.trim() });
                                setEditingName(false);
                            }}
                            onKeyDown={e => { if (e.key === 'Enter') e.target.blur(); if (e.key === 'Escape') setEditingName(false); }}
                        />
                    ) : (
                        <span className="canvas-config-title" onClick={() => { setStepName(displayStepName); setEditingName(true); }}>
                            {stepNumber > 0 ? `${stepNumber}. ` : ''}{displayStepName}
                            <HiOutlinePencil className="cpb-name-edit-icon" />
                        </span>
                    )}
                </div>
                <div className="canvas-config-header-right">
                    {nodeCount > 1 && (
                        <div className="canvas-config-nav">
                            <button className="canvas-config-nav-btn" disabled={nodeIndex <= 0} onClick={() => onNavigate?.('prev')}>‹</button>
                            <span className="canvas-config-nav-label">Next step</span>
                            <button className="canvas-config-nav-btn" disabled={nodeIndex >= nodeCount - 1} onClick={() => onNavigate?.('next')}>›</button>
                        </div>
                    )}
                    <button className="canvas-config-close" onClick={onClose}><HiX /></button>
                </div>
            </div>

            {/* ── Stepper Tabs ── */}
            <div className="cpb-stepper">
                {TABS.map((tab, idx) => {
                    const isComplete = idx === 0 ? setupComplete : idx === 1 ? configComplete : false;
                    const isCurrent = activeTab === idx;
                    const isAccessible = idx === 0 || (idx === 1 && setupComplete) || (idx === 2 && setupComplete);

                    return (
                        <button
                            key={tab}
                            className={`cpb-step ${isCurrent ? 'active' : ''} ${isComplete ? 'complete' : ''} ${!isAccessible ? 'disabled' : ''}`}
                            onClick={() => isAccessible && setActiveTab(idx)}
                            disabled={!isAccessible}
                        >
                            <span className="cpb-step-indicator">
                                {isComplete ? <HiCheck /> : idx + 1}
                            </span>
                            <span className="cpb-step-label">{tab}</span>
                            {idx < TABS.length - 1 && <span className="cpb-step-separator">›</span>}
                        </button>
                    );
                })}
            </div>

            {/* ── Tab Content ── */}
            <div className="cpb-tab-content">
                {/* ═══ SETUP TAB ═══ */}
                {activeTab === 0 && (
                    <div className="cpb-section">
                        {/* App selector — card style that opens browser modal */}
                        <div className="cpb-field">
                            <label className="cpb-label">{isTrigger ? 'Trigger App' : 'Action App'}</label>
                            <button
                                type="button"
                                className={`cpb-app-select-btn ${data.appKey ? 'selected' : ''}`}
                                onClick={onOpenAppBrowser}
                            >
                                <div className="cpb-app-select-icon">
                                    {data.appKey ? (
                                        <img src={data.iconUrl || appDetail?.logoUrl || `/icons/${data.appKey}.svg`} alt={data.appName || ''} className="app-logo-img" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }} />
                                    ) : (
                                        <HiOutlineBolt />
                                    )}
                                    <HiOutlineBolt style={{ display: 'none' }} />
                                </div>
                                <div className="cpb-app-select-text">
                                    <div className="cpb-app-select-name">
                                        {data.appName || data.appKey || 'Choose an app…'}
                                    </div>
                                    {!data.appKey && (
                                        <div className="cpb-app-select-hint">Browse all available apps</div>
                                    )}
                                </div>
                                <HiChevronRight className="cpb-app-select-chevron" />
                            </button>
                        </div>

                        {/* Account — Zapier-style card with Change + 3-dot menu */}
                        {data.appKey && !isNoAuthApp && (
                            <div className="cpb-field">
                                <label className="cpb-label">Account <span className="cpb-required">*</span></label>
                                
                                {appDetail?.hasPlatformKey && (
                                    <div className="cpb-admin-key-toggle" style={{ display: 'flex', gap: '16px', marginBottom: '12px', fontSize: '0.85rem' }}>
                                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', color: 'var(--text-primary)' }}>
                                            <input 
                                                type="radio" 
                                                checked={data.credentialSource !== 'ADMIN_KEY'} 
                                                onChange={() => {
                                                    updateNodeData(configNode.id, { credentialSource: 'PERSONAL' });
                                                }} 
                                            /> 
                                            Use My Own Account
                                        </label>
                                        <label style={{ display: 'flex', alignItems: 'center', gap: '6px', cursor: 'pointer', color: 'var(--text-primary)' }}>
                                            <input 
                                                type="radio" 
                                                checked={data.credentialSource === 'ADMIN_KEY'} 
                                                onChange={() => {
                                                    updateNodeData(configNode.id, { credentialSource: 'ADMIN_KEY', connectionId: null, account: null, accountName: '' });
                                                }} 
                                            /> 
                                            Use Crescendo's key
                                        </label>
                                    </div>
                                )}

                                {data.credentialSource === 'ADMIN_KEY' && appDetail?.hasPlatformKey ? (
                                    <div className="cpb-admin-key-pill" style={{ padding: '10px 14px', background: 'var(--bg-secondary)', border: '1px solid var(--border-secondary)', borderRadius: '6px', display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-secondary)', fontSize: '0.85rem' }}>
                                        <HiCheck style={{ color: '#22c55e' }} />
                                        <span>Using Crescendo's platform key</span>
                                    </div>
                                ) : data.connectionId && selectedConnections.length > 0 ? (() => {
                                    const conn = selectedConnections.find(c => c.id === data.connectionId);
                                    return (
                                        <div className="cpb-account-card">
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                                                <div className="cpb-app-select-icon" style={{ width: '24px', height: '24px' }}>
                                                    <img src={data.iconUrl || appDetail?.logoUrl || `/icons/${data.appKey}.svg`} alt="" className="app-logo-img" onError={(e) => { e.target.style.display = 'none'; e.target.nextSibling.style.display = 'block'; }} />
                                                    <HiOutlineBolt style={{ display: 'none' }} />
                                                </div>
                                                <div className="cpb-account-info">
                                                    <span className="cpb-account-name">{conn?.accountEmail || conn?.accountDisplayName || conn?.name || data.appName}</span>
                                                    {conn?.name && <span className="cpb-account-hint">{conn.name}</span>}
                                                </div>
                                            </div>
                                            <button type="button" className="cpb-account-change" onClick={() => {
                                                updateNodeData(configNode.id, { connectionId: null, account: null, accountName: '' });
                                            }}>Change</button>
                                            <div className="cpb-account-menu-wrap" ref={accountMenuRef}>
                                                <button type="button" className="cpb-account-dots" onClick={() => setAccountMenuOpen(!accountMenuOpen)}>⋮</button>
                                                {accountMenuOpen && (
                                                    <div className="cpb-account-menu">
                                                        <button type="button" onClick={async () => {
                                                            setAccountMenuOpen(false);
                                                            const addToast = useToastStore.getState().addToast;
                                                            addToast('Testing connection…', 'info', 5000);
                                                            try {
                                                                const result = await connectionsApi.test(data.connectionId);
                                                                if (result.success) {
                                                                    addToast(result.message || 'Connection works!', 'success');
                                                                } else {
                                                                    addToast(result.message || 'Connection test failed', 'error', 5000);
                                                                }
                                                            } catch (err) {
                                                                addToast('Test failed: ' + (err?.response?.data?.message || err.message), 'error', 5000);
                                                            }
                                                        }}>
                                                            Test connection
                                                        </button>
                                                        <button type="button" onClick={() => { setAccountMenuOpen(false); handleNewConnection(data.connectionId); }}>
                                                            Reconnect
                                                        </button>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    );
                                })() : (
                                    <>
                                        {selectedConnections.length > 0 ? (
                                            <SearchableSelect
                                                options={connectionOptions}
                                                value={data.connectionId || ''}
                                                onChange={(id) => {
                                                    const conn = selectedConnections.find((c) => c.id === id);
                                                    updateNodeData(configNode.id, {
                                                        connectionId: id, account: id,
                                                        accountName: conn?.name || '',
                                                    });
                                                }}
                                                placeholder="Select account…"
                                                searchable={false}
                                            />
                                        ) : (
                                            <div className="cpb-empty-connections">
                                                <p>No accounts connected for <strong>{data.appName || data.appKey}</strong></p>
                                            </div>
                                        )}
                                        <button type="button" className={`cpb-browse-btn ${selectedConnections.length === 0 ? 'cpb-connect-cta' : ''}`} onClick={() => handleNewConnection()}>
                                            <HiPlus style={{ fontSize: '0.7rem' }} />
                                            {selectedConnections.length === 0 ? `Connect ${data.appName || data.appKey}` : 'Connect new account'}
                                        </button>
                                    </>
                                )}
                            </div>
                        )}

                        {/* No-auth app notice */}
                        {data.appKey && isNoAuthApp && (
                            <div className="cpb-field">
                                <div className="cpb-no-auth-notice">
                                    <HiCheck style={{ color: '#22c55e', fontSize: '0.85rem' }} />
                                    <span>No account connection needed</span>
                                </div>
                            </div>
                        )}

                        {/* Action/Trigger selector — show when app selected AND (connection exists OR no-auth) */}
                        {data.appKey && hasConnection && (
                            <div className="cpb-field">
                                <label className="cpb-label">{isTrigger ? 'Trigger Event' : 'Action'}</label>
                                <SearchableSelect
                                    options={operationOptions}
                                    value={actionOrTriggerKey || ''}
                                    onChange={handleOperationSelect}
                                    placeholder={isTrigger ? 'Choose trigger…' : 'Choose action…'}
                                />
                            </div>
                        )}

                        {/* Continue button */}
                        {setupComplete && (
                            <button
                                type="button"
                                className="cpb-continue-btn"
                                onClick={() => setActiveTab(1)}
                            >
                                Continue
                            </button>
                        )}
                    </div>
                )}

                {/* ═══ CONFIGURE TAB ═══ */}
                {activeTab === 1 && (
                    <div className="cpb-section">
                        {configSchema.length === 0 ? (
                            <div className="cpb-empty-config">
                                No configuration needed for this {isTrigger ? 'trigger' : 'action'}.
                            </div>
                        ) : (
                            configSchema.map((field) => (
                                <div key={field.key} className="cpb-field">
                                    <label className="cpb-label">
                                        {field.label}
                                        {field.required && <span className="cpb-required">*</span>}
                                    </label>
                                    <DynamicField
                                        field={field}
                                        appKey={data.appKey}
                                        connectionId={data.connectionId}
                                        config={data.configuration || {}}
                                        value={(data.configuration || {})[field.key]}
                                        onChange={(val) => updateConfig(field.key, val)}
                                        availableVariables={previousStepVariables}
                                    />
                                    {field.helpText && (
                                        <span className="cpb-help">{field.helpText}</span>
                                    )}
                                </div>
                            ))
                        )}

                        {/* Continue to Test */}
                        <button
                            type="button"
                            className="cpb-continue-btn"
                            onClick={() => setActiveTab(2)}
                        >
                            Continue
                        </button>
                    </div>
                )}

                {/* ═══ TEST TAB ═══ */}
                {activeTab === 2 && (
                    <div className="cpb-section">
                        <div className="cpb-test-intro">
                            {isTrigger
                                ? 'Test your trigger to find recent records from your connected account.'
                                : 'Test your action to verify it works with the configuration above.'}
                        </div>
                        <TestResultPanel
                            appKey={data.appKey}
                            actionKey={data.actionKey}
                            connectionId={data.connectionId}
                            configuration={data.configuration || {}}
                            isTrigger={isTrigger}
                        />
                    </div>
                )}
            </div>

            {/* ── Footer ── */}
            <div className="canvas-config-footer">
                {isTrigger ? (
                    // Trigger nodes: clear contents but keep node position
                    <button
                        className="canvas-config-btn"
                        title="Clear this trigger's configuration"
                        onClick={onClear || onDelete}
                        style={{ color: 'var(--text-tertiary)' }}
                    >
                        <HiOutlineTrash />
                        <span style={{ fontSize: '0.72rem', marginLeft: 4 }}>Clear</span>
                    </button>
                ) : (
                    <button className="canvas-config-btn danger" title="Remove this action step" onClick={onDelete}>
                        <HiOutlineTrash />
                    </button>
                )}
                <button className="canvas-config-btn canvas-config-btn-save" onClick={() => {
                    const appMeta = catalogApps.find(a => a.appKey === data.appKey);
                    const appName = appMeta?.name || data.appName || data.appKey;
                    if (!isTrigger && data.appKey && data.actionKey) {
                        const actionName = selectedActionOptions.find(a => a.actionKey === data.actionKey)?.name || data.actionKey;
                        updateNodeData(configNode.id, { label: `${appName} · ${actionName}`, actionName });
                    } else if (isTrigger && data.appKey && (data.triggerKey || data.actionKey)) {
                        const triggerKey = data.triggerKey || data.actionKey;
                        const triggerName = selectedTriggerOptions.find(t => t.triggerKey === triggerKey)?.name || triggerKey;
                        updateNodeData(configNode.id, { label: `${appName} · ${triggerName}`, triggerName });
                    }
                    // Persist to backend via parent's handleSave, then close
                    if (onSaveAndClose) {
                        onSaveAndClose();
                    } else {
                        onClose?.();
                    }
                }}>
                    <HiCheck style={{ fontSize: '1rem' }} /> Save & Close
                </button>
            </div>
        </div>
    );
}
