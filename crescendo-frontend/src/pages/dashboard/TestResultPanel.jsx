import { useEffect, useMemo, useState } from 'react';
import {
  HiOutlinePlay, HiOutlineRefresh, HiCheckCircle, HiXCircle,
  HiOutlineDownload, HiOutlineClipboardCopy, HiOutlineExclamation, HiOutlineShieldCheck,
  HiOutlineSparkles, HiOutlineDocumentText, HiOutlineArrowCircleRight
} from 'react-icons/hi';
import { stepTestApi } from '../../api/workflowApi';
import './TestResultPanel.css';

/**
 * A safe workflow-step test panel.
 * Separates setup checks, trigger sample fetching, read-only sample fetching, and live execution.
 */
export default function TestResultPanel({
    appKey, actionKey, triggerKey, connectionId, configuration,
    isTrigger = false, initialInputData = null, availablePreviousSteps = [], onSaveSampleData = null,
}) {
    const [result, setResult] = useState(null);
    const [checking, setChecking] = useState(false);
    const [fetchingSample, setFetchingSample] = useState(false);
    const [runningLive, setRunningLive] = useState(false);
    const [copied, setCopied] = useState(false);
    const [savedSample, setSavedSample] = useState(false);
    const [inputText, setInputText] = useState(() => JSON.stringify(initialInputData ?? {}, null, 2));
    const [inputError, setInputError] = useState('');
    const [showLiveConfirmation, setShowLiveConfirmation] = useState(false);
    const [liveAcknowledged, setLiveAcknowledged] = useState(false);
    const [activeResultTab, setActiveResultTab] = useState('checks'); // 'checks' | 'dataIn' | 'dataOut'

    const operationKey = isTrigger ? triggerKey : actionKey;
    const canCheck = Boolean(appKey && operationKey);
    const liveAllowed = !isTrigger && result?.testContract?.liveTestAllowed === true;
    const canFetchReadSample = !isTrigger
        && result?.success
        && result?.testContract?.setupPolicy === 'READ_SAMPLE'
        && result?.testContract?.sideEffect === 'NONE';

    useEffect(() => {
        if (initialInputData && Object.keys(initialInputData).length > 0) {
            setInputText(JSON.stringify(initialInputData, null, 2));
        }
    }, [initialInputData]);

    const requestData = () => {
        try {
            const inputData = inputText.trim() ? JSON.parse(inputText) : {};
            if (!inputData || Array.isArray(inputData) || typeof inputData !== 'object') {
                throw new Error('Sample data must be a JSON object.');
            }
            setInputError('');
            return {
                appKey,
                actionKey: isTrigger ? null : actionKey,
                triggerKey: isTrigger ? (triggerKey || actionKey) : null,
                isTrigger: Boolean(isTrigger),
                connectionId: connectionId || null,
                configuration: configuration || {},
                inputData,
                acknowledgeLiveRun: Boolean(liveAcknowledged),
            };
        } catch (error) {
            setInputError(error.message || 'Enter valid JSON sample data.');
            return null;
        }
    };

    const checkSetup = async () => {
        if (!canCheck) return;
        const request = requestData();
        if (!request) return;
        setChecking(true);
        setResult(null);
        setCopied(false);
        setSavedSample(false);
        setShowLiveConfirmation(false);
        try {
            const res = await stepTestApi.validate(request);
            setResult(res);
            setActiveResultTab('checks');
        } catch (error) {
            setResult({ success: false, error: error.response?.data?.error || error.message, checks: [], mode: 'SETUP_CHECK' });
            setActiveResultTab('checks');
        } finally {
            setChecking(false);
        }
    };

    const fetchTriggerSample = async () => {
        if (!canCheck) return;
        const request = requestData();
        if (!request) return;
        setFetchingSample(true);
        setResult(null);
        setCopied(false);
        setSavedSample(false);
        try {
            const res = await stepTestApi.triggerSample(request);
            setResult(res);
            setActiveResultTab('dataOut');
            if (res.success && res.data && onSaveSampleData) {
                onSaveSampleData(res.data);
                setSavedSample(true);
            }
        } catch (error) {
            setResult({ success: false, error: error.response?.data?.error || error.message, checks: [], mode: 'TRIGGER_SAMPLE' });
            setActiveResultTab('checks');
        } finally {
            setFetchingSample(false);
        }
    };

    const fetchReadSample = async () => {
        const request = requestData();
        if (!request) return;
        setFetchingSample(true);
        setCopied(false);
        setSavedSample(false);
        try {
            const res = await stepTestApi.readSample(request);
            setResult(res);
            setActiveResultTab('dataOut');
            if (res.success && res.data && onSaveSampleData) {
                onSaveSampleData(res.data);
                setSavedSample(true);
            }
        } catch (error) {
            setResult({ success: false, error: error.response?.data?.error || error.message, checks: [], mode: 'READ_SAMPLE' });
        } finally {
            setFetchingSample(false);
        }
    };

    const runLive = async () => {
        const request = requestData();
        if (!request || !liveAcknowledged) return;
        setRunningLive(true);
        setCopied(false);
        setSavedSample(false);
        try {
            const res = await stepTestApi.liveRun(request);
            setResult(res);
            setShowLiveConfirmation(false);
            setLiveAcknowledged(false);
            setActiveResultTab('dataOut');
            if (res.success && res.data && onSaveSampleData) {
                onSaveSampleData(res.data);
                setSavedSample(true);
            }
        } catch (error) {
            setResult({ success: false, error: error.response?.data?.error || error.message, checks: [], mode: 'LIVE_RUN' });
        } finally {
            setRunningLive(false);
        }
    };

    const handleUsePreviousStepSample = (step) => {
        if (!step) return;
        const samplePayload = step.sampleData || { id: `sample_${step.stepIndex}`, note: `Sample data from ${step.name}` };
        const formatted = {
            steps: {
                [step.stepIndex]: samplePayload,
            },
            ...samplePayload,
        };
        setInputText(JSON.stringify(formatted, null, 2));
        setInputError('');
    };

    const handleSaveSampleToWorkflow = () => {
        if (result?.data && onSaveSampleData) {
            onSaveSampleData(result.data);
            setSavedSample(true);
            window.setTimeout(() => setSavedSample(false), 2500);
        }
    };

    const handleDownloadJSON = () => {
        const dataToExport = activeResultTab === 'dataIn' ? (result?.data?.dataIn || result?.data) : result?.data;
        if (!dataToExport) return;
        const blob = new Blob([JSON.stringify(dataToExport, null, 2)], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        const resultKind = result.mode === 'LIVE_RUN'
            ? 'live-run'
            : result.mode === 'READ_SAMPLE' ? 'read-sample' : 'setup-check';
        anchor.download = `${appKey}-${operationKey}-${resultKind}.json`;
        document.body.appendChild(anchor);
        anchor.click();
        document.body.removeChild(anchor);
        URL.revokeObjectURL(url);
    };

    const handleCopyJSON = () => {
        const dataToCopy = activeResultTab === 'dataIn' ? (result?.data?.dataIn || result?.data) : result?.data;
        if (!dataToCopy) return;
        navigator.clipboard.writeText(JSON.stringify(dataToCopy, null, 2));
        setCopied(true);
        window.setTimeout(() => setCopied(false), 2000);
    };

    const dataInRows = useMemo(() => flattenData(result?.data?.dataIn || (result?.mode === 'SETUP_CHECK' ? result?.data?.configuration : null)), [result]);
    const dataOutRows = useMemo(() => flattenData(result?.mode === 'SETUP_CHECK' ? null : result?.data), [result]);

    return (
        <div className="trp-container">
            <div className="trp-safety-note">
                <HiOutlineShieldCheck aria-hidden="true" />
                <span>
                    {isTrigger ? (
                        <><strong>Test Trigger</strong> retrieves sample event records without activating live workflows.</>
                    ) : (
                        <><strong>Check setup</strong> verifies your connection, fields, and selected resources without changing data.</>
                    )}
                </span>
            </div>

            {/* Quick Upstream Sample Selector */}
            {!isTrigger && availablePreviousSteps.length > 0 && (
                <div className="trp-upstream-selector">
                    <span className="trp-upstream-label">Quick sample data:</span>
                    <div className="trp-upstream-btns">
                        {availablePreviousSteps.map((step) => (
                            <button
                                key={step.stepIndex}
                                type="button"
                                className="trp-upstream-btn"
                                onClick={() => handleUsePreviousStepSample(step)}
                                title={`Use sample output from Step ${step.stepIndex}: ${step.name}`}
                            >
                                <HiOutlineSparkles /> Step {step.stepIndex} ({step.name})
                            </button>
                        ))}
                    </div>
                </div>
            )}

            {!isTrigger && (
                <>
                    <label className="trp-input-label" htmlFor={`test-input-${operationKey}`}>
                        Sample input data <span>Optional — use earlier-step data to preview mappings</span>
                    </label>
                    <textarea
                        id={`test-input-${operationKey}`}
                        className={`trp-input-json ${inputError ? 'has-error' : ''}`}
                        value={inputText}
                        onChange={(event) => setInputText(event.target.value)}
                        spellCheck="false"
                        placeholder='{\n  "email": "user@example.com",\n  "amount": 99.00\n}'
                        aria-describedby={inputError ? `test-input-error-${operationKey}` : undefined}
                    />
                    {inputError && <p id={`test-input-error-${operationKey}`} className="trp-input-error">{inputError}</p>}
                </>
            )}

            {/* Main Action Buttons */}
            <div className="trp-main-actions">
                {isTrigger ? (
                    <button
                        type="button"
                        className={`trp-test-btn trigger-btn ${fetchingSample ? 'testing' : ''}`}
                        onClick={fetchTriggerSample}
                        disabled={fetchingSample || !canCheck}
                    >
                        {fetchingSample ? <LoadingLabel label="Fetching sample record…" /> : <><HiOutlineSparkles className="trp-test-btn-icon" /> Test Trigger / Fetch Sample Record</>}
                    </button>
                ) : (
                    <button
                        type="button"
                        className={`trp-test-btn ${checking ? 'testing' : ''}`}
                        onClick={checkSetup}
                        disabled={checking || fetchingSample || runningLive || !canCheck}
                    >
                        {checking ? <LoadingLabel label="Checking setup…" /> : <><HiOutlineShieldCheck className="trp-test-btn-icon" /> Check setup</>}
                    </button>
                )}
            </div>

            {result && (
                <div className="trp-result">
                    <div className={`trp-result-header ${result.success ? 'success' : 'error'}`}>
                        {result.success ? <HiCheckCircle className="trp-result-icon" /> : <HiXCircle className="trp-result-icon" />}
                        {result.mode === 'LIVE_RUN'
                            ? (result.success ? 'Live action completed' : 'Live action failed')
                            : result.mode === 'TRIGGER_SAMPLE'
                                ? (result.success ? 'Trigger sample record loaded' : 'Could not load trigger sample')
                                : result.mode === 'READ_SAMPLE'
                                    ? (result.success ? 'Read-only sample fetched' : 'Could not fetch a read-only sample')
                                    : (result.success ? 'Setup checked safely' : 'Setup needs attention')}
                    </div>

                    {/* Result Navigation Tabs */}
                    {result.success && (
                        <div className="trp-nav-tabs">
                            <button
                                type="button"
                                className={`trp-nav-tab ${activeResultTab === 'checks' ? 'active' : ''}`}
                                onClick={() => setActiveResultTab('checks')}
                            >
                                <HiCheckCircle /> Checks {result.checks?.length > 0 && `(${result.checks.length})`}
                            </button>
                            {!isTrigger && (
                                <button
                                    type="button"
                                    className={`trp-nav-tab ${activeResultTab === 'dataIn' ? 'active' : ''}`}
                                    onClick={() => setActiveResultTab('dataIn')}
                                >
                                    <HiOutlineDocumentText /> Data In (Preview)
                                </button>
                            )}
                            {(result.mode !== 'SETUP_CHECK' || result.data?.dataIn) && (
                                <button
                                    type="button"
                                    className={`trp-nav-tab ${activeResultTab === 'dataOut' ? 'active' : ''}`}
                                    onClick={() => setActiveResultTab('dataOut')}
                                >
                                    <HiOutlineArrowCircleRight /> Data Out
                                </button>
                            )}
                        </div>
                    )}

                    {/* 1. CHECKS TAB */}
                    {activeResultTab === 'checks' && result.checks?.length > 0 && (
                        <div className="trp-checks" aria-label="Setup check results">
                            {result.checks.map((check) => (
                                <div key={check.id} className={`trp-check trp-check-${check.status.toLowerCase()}`}>
                                    {check.status === 'PASS' ? <HiCheckCircle /> : <HiOutlineExclamation />}
                                    <div><strong>{check.label}</strong><span>{check.detail}</span></div>
                                </div>
                            ))}
                        </div>
                    )}

                    {/* 2. DATA IN PREVIEW TAB */}
                    {activeResultTab === 'dataIn' && (
                        <div className="trp-tab-content">
                            <div className="trp-tab-note">
                                Resolved configuration values that will be sent to the API:
                            </div>
                            {dataInRows.length > 0 ? (
                                dataInRows.length <= 30 ? (
                                    <div className="trp-data">
                                        {dataInRows.map((row) => (
                                            <div key={row.key} className="trp-data-row">
                                                <span className="trp-data-key">{row.key}</span>
                                                <span className="trp-data-value">{row.value}</span>
                                            </div>
                                        ))}
                                    </div>
                                ) : <pre className="trp-raw">{JSON.stringify(result.data?.dataIn || result.data?.configuration, null, 2)}</pre>
                            ) : (
                                <div className="trp-empty-tab">No configuration fields mapped.</div>
                            )}
                        </div>
                    )}

                    {/* 3. DATA OUT / SAMPLE TAB */}
                    {activeResultTab === 'dataOut' && (
                        <div className="trp-tab-content">
                            {dataOutRows.length > 0 ? (
                                dataOutRows.length <= 30 ? (
                                    <div className="trp-data">
                                        {dataOutRows.map((row) => (
                                            <div key={row.key} className="trp-data-row">
                                                <span className="trp-data-key">{row.key}</span>
                                                <span className="trp-data-value">{row.value}</span>
                                            </div>
                                        ))}
                                    </div>
                                ) : <pre className="trp-raw">{JSON.stringify(result.data, null, 2)}</pre>
                            ) : (
                                <pre className="trp-raw">{JSON.stringify(result.data || {}, null, 2)}</pre>
                            )}
                        </div>
                    )}

                    {!result.success && <div className="trp-error-body">{result.error}</div>}

                    {/* Live Action & Read Sample Action Buttons */}
                    {liveAllowed && result.success && !showLiveConfirmation && (
                        <button type="button" className="trp-live-open" onClick={() => setShowLiveConfirmation(true)}>
                            <HiOutlinePlay /> Run live action…
                        </button>
                    )}
                    {canFetchReadSample && (
                        <button type="button" className="trp-read-sample" onClick={fetchReadSample} disabled={fetchingSample || runningLive}>
                            {fetchingSample ? <LoadingLabel label="Fetching sample…" /> : <><HiOutlineDownload /> Fetch read-only sample</>}
                        </button>
                    )}
                    {showLiveConfirmation && (
                        <div className="trp-live-confirmation">
                            <HiOutlineExclamation aria-hidden="true" />
                            <div>
                                <strong>This is not a setup check.</strong>
                                <p>{result.testContract?.liveTestWarning || 'This will perform the configured action in your connected app.'}</p>
                                <label>
                                    <input type="checkbox" checked={liveAcknowledged} onChange={(event) => setLiveAcknowledged(event.target.checked)} />
                                    I understand this may change external data.
                                </label>
                                <div className="trp-live-actions">
                                    <button type="button" onClick={() => { setShowLiveConfirmation(false); setLiveAcknowledged(false); }}>Cancel</button>
                                    <button type="button" className="danger" onClick={runLive} disabled={!liveAcknowledged || runningLive}>
                                        {runningLive ? <LoadingLabel label="Running live action…" /> : 'Run live action'}
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Footer Actions */}
                    <div className="trp-actions">
                        <button
                            type="button"
                            className="trp-retry"
                            onClick={isTrigger ? fetchTriggerSample : checkSetup}
                            disabled={checking || fetchingSample || runningLive}
                        >
                            <HiOutlineRefresh /> {isTrigger ? 'Fetch again' : 'Check again'}
                        </button>
                        {result.data && (
                            <>
                                {onSaveSampleData && (
                                    <button type="button" className="trp-save-sample-btn" onClick={handleSaveSampleToWorkflow}>
                                        <HiCheckCircle /> {savedSample ? 'Sample Saved!' : 'Use as Sample'}
                                    </button>
                                )}
                                <button type="button" className="trp-download-btn" onClick={handleDownloadJSON}>
                                    <HiOutlineDownload /> Download JSON
                                </button>
                                <button type="button" className="trp-copy-btn" onClick={handleCopyJSON}>
                                    {copied ? <><HiCheckCircle /> Copied</> : <><HiOutlineClipboardCopy /> Copy</>}
                                </button>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

function LoadingLabel({ label }) {
    return <><span className="trp-loading-inline"><i /><i /><i /></span>{label}</>;
}

function flattenData(obj) {
    if (!obj || typeof obj !== 'object') return [];
    const rows = [];
    const visit = (value, prefix = '') => Object.entries(value).forEach(([key, child]) => {
        const fullKey = prefix ? `${prefix}.${key}` : key;
        if (child && typeof child === 'object' && !Array.isArray(child)) visit(child, fullKey);
        else rows.push({ key: fullKey, value: Array.isArray(child) ? JSON.stringify(child) : String(child ?? '') });
    });
    visit(obj);
    return rows;
}
