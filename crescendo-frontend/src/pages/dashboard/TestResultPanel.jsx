import { useState } from 'react';
import {
  HiOutlinePlay, HiOutlineRefresh, HiCheckCircle, HiXCircle,
  HiOutlineDownload, HiOutlineClipboardCopy
} from 'react-icons/hi';
import { stepTestApi } from '../../api/workflowApi';
import './TestResultPanel.css';

/**
 * TestResultPanel — structured test execution & result display.
 *
 * Features:
 *   - Test execution with loading state
 *   - Structured key-value display or raw JSON
 *   - Download results as JSON file
 *   - Copy results to clipboard
 *
 * Props:
 *   appKey        — app key string
 *   actionKey     — action/trigger key string
 *   connectionId  — connection UUID
 *   configuration — config object
 *   isTrigger     — boolean
 */
export default function TestResultPanel({ appKey, actionKey, connectionId, configuration, isTrigger = false }) {
    const [result, setResult] = useState(null);
    const [testing, setTesting] = useState(false);
    const [copied, setCopied] = useState(false);

    const canTest = appKey && actionKey;

    const runTest = async () => {
        if (!canTest) return;
        setTesting(true);
        setResult(null);
        setCopied(false);
        try {
            const res = await stepTestApi.test({ appKey, actionKey, connectionId, configuration });
            setResult(res);
        } catch (err) {
            setResult({ success: false, error: err.response?.data?.error || err.message });
        } finally {
            setTesting(false);
        }
    };

    const handleDownloadJSON = () => {
        if (!result?.data) return;
        const json = JSON.stringify(result.data, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${appKey}-${actionKey}-results.json`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    };

    const handleCopyJSON = () => {
        if (!result?.data) return;
        navigator.clipboard.writeText(JSON.stringify(result.data, null, 2));
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    // Flatten an object for key-value display
    const flattenData = (obj) => {
        if (!obj || typeof obj !== 'object') return [];
        const rows = [];
        const recurse = (o, prefix = '') => {
            Object.entries(o).forEach(([key, val]) => {
                const fullKey = prefix ? `${prefix}.${key}` : key;
                if (val && typeof val === 'object' && !Array.isArray(val)) {
                    recurse(val, fullKey);
                } else {
                    rows.push({ key: fullKey, value: Array.isArray(val) ? JSON.stringify(val) : String(val ?? '') });
                }
            });
        };
        recurse(obj);
        return rows;
    };

    return (
        <div className="trp-container">
            {/* Test button */}
            <button
                type="button"
                className={`trp-test-btn ${testing ? 'testing' : ''}`}
                onClick={runTest}
                disabled={testing || !canTest}
            >
                {testing ? (
                    <>
                        <div className="trp-loading" style={{ padding: 0 }}>
                            <span className="trp-loading-bar" />
                            <span className="trp-loading-bar" />
                            <span className="trp-loading-bar" />
                            <span className="trp-loading-bar" />
                        </div>
                        Testing…
                    </>
                ) : (
                    <>
                        <HiOutlinePlay className="trp-test-btn-icon" />
                        {isTrigger ? 'Find Records' : 'Test Action'}
                    </>
                )}
            </button>

            {/* Result */}
            {result && (
                <div className="trp-result">
                    {/* Header banner */}
                    <div className={`trp-result-header ${result.success ? 'success' : 'error'}`}>
                        {result.success ? (
                            <HiCheckCircle className="trp-result-icon" />
                        ) : (
                            <HiXCircle className="trp-result-icon" />
                        )}
                        {result.success ? 'Test successful' : 'Test failed'}
                    </div>

                    {/* Body */}
                    {result.success ? (
                        (() => {
                            const rows = flattenData(result.data);
                            if (rows.length > 0 && rows.length <= 30) {
                                // Structured key-value view
                                return (
                                    <div className="trp-data">
                                        {rows.map((row) => (
                                            <div key={row.key} className="trp-data-row">
                                                <span className="trp-data-key">{row.key}</span>
                                                <span className="trp-data-value">{row.value}</span>
                                            </div>
                                        ))}
                                    </div>
                                );
                            }
                            // Fallback to raw JSON
                            return (
                                <pre className="trp-raw">
                                    {JSON.stringify(result.data, null, 2)}
                                </pre>
                            );
                        })()
                    ) : (
                        <div className="trp-error-body">{result.error}</div>
                    )}

                    {/* Action buttons */}
                    <div className="trp-actions">
                        <button type="button" className="trp-retry" onClick={runTest}>
                            <HiOutlineRefresh /> {result.success ? 'Test Again' : 'Retry'}
                        </button>

                        {result.success && result.data && (
                            <>
                                <button type="button" className="trp-download-btn" onClick={handleDownloadJSON}>
                                    <HiOutlineDownload /> Download JSON
                                </button>
                                <button type="button" className="trp-copy-btn" onClick={handleCopyJSON}>
                                    {copied
                                        ? <><HiCheckCircle /> Copied!</>
                                        : <><HiOutlineClipboardCopy /> Copy</>
                                    }
                                </button>
                            </>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
