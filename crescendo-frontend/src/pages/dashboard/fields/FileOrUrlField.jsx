import React, { useState, useRef, useMemo } from 'react';
import {
    HiUpload,
    HiLink,
    HiX,
    HiDocumentText,
    HiFilm,
    HiPhotograph,
    HiMusicNote,
    HiCloudDownload,
    HiCheckCircle,
} from 'react-icons/hi';
import useToastStore from '../../../store/toastStore';
import api from '../../../api/axios';
import { VariableInsertButton } from '../ConfigPanelBody';

const GOOGLE_DRIVE_REGEX = /https?:\/\/drive\.google\.com\/(?:file\/d\/|open\?id=|uc\?(?:[^&]*&)*id=)([a-zA-Z0-9_-]+)/;

/**
 * Format bytes into human-readable string (e.g., 4.2 MB)
 */
function formatBytes(bytes) {
    if (!bytes || bytes <= 0) return '';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

/**
 * Get icon based on mime type or accept filter
 */
function getFileIcon(mimeType = '', accept = '') {
    const combined = (mimeType + ' ' + accept).toLowerCase();
    if (combined.includes('video')) return <HiFilm />;
    if (combined.includes('image') || combined.includes('photo')) return <HiPhotograph />;
    if (combined.includes('audio') || combined.includes('voice')) return <HiMusicNote />;
    return <HiDocumentText />;
}

/**
 * Dual-Mode File & URL / Stream Input Component.
 * Supports direct file drag-and-drop upload and Google Drive / S3 / CDN streaming links.
 */
export function FileOrUrlField({ field, value, onChange, availableVariables }) {
    const fileInputRef = useRef(null);
    const addToast = useToastStore(s => s.addToast);

    // Determine initial active mode (upload vs url)
    const initialMode = useMemo(() => {
        if (typeof value === 'string' && (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('{{') || value.includes('drive.google.com'))) {
            return 'url';
        }
        if (value && typeof value === 'object' && value.storageKey) {
            return 'upload';
        }
        return 'upload';
    }, []);

    const [mode, setMode] = useState(initialMode);
    const [isDragging, setIsDragging] = useState(false);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [uploadStats, setUploadStats] = useState({ loaded: 0, total: 0 });
    const [uploadFileName, setUploadFileName] = useState('');

    const isGoogleDriveLink = useMemo(() => {
        return typeof value === 'string' && GOOGLE_DRIVE_REGEX.test(value);
    }, [value]);

    const isVariable = useMemo(() => {
        return typeof value === 'string' && value.includes('{{');
    }, [value]);

    const acceptStr = field.accept || '*/*';
    const maxSizeMB = field.maxSizeMB || 50;

    const processFile = async (file) => {
        if (file.size > maxSizeMB * 1024 * 1024) {
            addToast(`File size (${(file.size / (1024 * 1024)).toFixed(1)}MB) exceeds limit of ${maxSizeMB}MB. Use URL/Drive link for larger files.`, 'error');
            return;
        }

        setIsUploading(true);
        setUploadProgress(0);
        setUploadStats({ loaded: 0, total: file.size });
        setUploadFileName(file.name);

        try {
            const formData = new FormData();
            formData.append('file', file);
            const consumptionModel = field.consumptionModel || 'RELAY';
            formData.append('consumptionModel', consumptionModel);
            if (field.maxSizeMB) formData.append('maxSizeMB', field.maxSizeMB);

            const res = await api.post('/files/upload', formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
                onUploadProgress: (progressEvent) => {
                    if (progressEvent.total) {
                        const percent = Math.min(100, Math.round((progressEvent.loaded * 100) / progressEvent.total));
                        setUploadProgress(percent);
                        setUploadStats({ loaded: progressEvent.loaded, total: progressEvent.total });
                    }
                },
            });

            onChange(res.data);
            addToast(`"${file.name}" uploaded successfully`, 'success');
        } catch (err) {
            console.error('File upload error:', err);
            const msg = err.response?.data?.message || err.message || 'Upload failed';
            addToast(`Failed to upload file: ${msg}`, 'error');
        } finally {
            setIsUploading(false);
            setUploadProgress(0);
            setUploadStats({ loaded: 0, total: 0 });
        }
    };

    const handleFileChange = (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        processFile(file);
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

    const handleRemoveFile = (e) => {
        e.stopPropagation();
        onChange(null);
        if (fileInputRef.current) fileInputRef.current.value = '';
    };

    const handleInsertVariable = (variableText) => {
        const current = typeof value === 'string' ? value : '';
        onChange(current + variableText);
    };

    // If an object is stored from previous upload
    const uploadedObj = value && typeof value === 'object' ? value : null;
    const stringVal = typeof value === 'string' ? value : '';

    return (
        <div className="cpb-file-or-url-wrapper">
            {/* Mode Segmented Switcher */}
            <div className="cpb-media-mode-toggle">
                <button
                    type="button"
                    className={`cpb-media-mode-btn ${mode === 'upload' ? 'active' : ''}`}
                    onClick={() => setMode('upload')}
                >
                    <HiUpload />
                    <span>Upload File</span>
                </button>
                <button
                    type="button"
                    className={`cpb-media-mode-btn ${mode === 'url' ? 'active' : ''}`}
                    onClick={() => setMode('url')}
                >
                    <HiLink />
                    <span>URL / Drive Link</span>
                </button>
            </div>

            {/* ── MODE 1: Direct File Upload ── */}
            {mode === 'upload' && (
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
                            <div className="cpb-file-progress-bar">
                                <div className="cpb-file-progress-fill" style={{ width: `${uploadProgress}%` }} />
                            </div>
                            <div className="cpb-file-uploading-row">
                                <span className="cpb-file-uploading-name" title={uploadFileName}>
                                    {uploadProgress === 100 ? 'Processing file...' : `Uploading ${uploadFileName || 'file'}`}
                                </span>
                                <div className="cpb-file-uploading-metrics">
                                    {uploadStats.total > 0 && (
                                        <span className="cpb-file-uploading-bytes">
                                            {formatBytes(uploadStats.loaded)} / {formatBytes(uploadStats.total)}
                                        </span>
                                    )}
                                    <span className="cpb-file-uploading-percent">{uploadProgress}%</span>
                                </div>
                            </div>
                        </div>
                    ) : uploadedObj ? (
                        <div className="cpb-file-selected">
                            <div className="cpb-file-icon">
                                {getFileIcon(uploadedObj.contentType, acceptStr)}
                            </div>
                            <div className="cpb-file-info">
                                <span className="cpb-file-name">{uploadedObj.name || 'Uploaded File'}</span>
                                <span className="cpb-file-meta">
                                    {uploadedObj.sizeBytes ? formatBytes(uploadedObj.sizeBytes) : 'Stored'} · {uploadedObj.contentType || 'Ready'}
                                </span>
                            </div>
                            <button
                                type="button"
                                className="cpb-file-remove"
                                onClick={handleRemoveFile}
                                title="Remove file"
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
                                {getFileIcon('', acceptStr)}
                            </div>
                            <div className="cpb-file-dropzone-text">
                                <span>Click to upload</span> or drag and drop
                            </div>
                            <div className="cpb-file-dropzone-hint">
                                Max {maxSizeMB}MB · {acceptStr === '*/*' ? 'Any file type' : acceptStr}
                            </div>
                            <div className="cpb-file-stream-tip">
                                For files &gt;{maxSizeMB}MB, switch to <strong>URL / Drive Link</strong> to stream directly.
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* ── MODE 2: URL, Stream & Google Drive Link ── */}
            {mode === 'url' && (
                <div className="cpb-media-url-container">
                    <div className="cpb-input-with-vars">
                        <input
                            type="text"
                            className="cpb-input"
                            value={stringVal}
                            placeholder={field.placeholder || "https://drive.google.com/file/d/... or {{steps.1.fileUrl}}"}
                            onChange={(e) => onChange(e.target.value)}
                        />
                        {availableVariables && availableVariables.length > 0 && (
                            <VariableInsertButton
                                availableVariables={availableVariables}
                                onInsert={handleInsertVariable}
                            />
                        )}
                    </div>

                    {/* Google Drive Detection Badge */}
                    {isGoogleDriveLink && (
                        <div className="cpb-gdrive-badge">
                            <HiCloudDownload className="cpb-gdrive-icon" />
                            <span>Google Drive link detected — automatic chunked stream active</span>
                        </div>
                    )}

                    {/* Dynamic Variable Detection Badge */}
                    {isVariable && (
                        <div className="cpb-var-badge">
                            <HiCheckCircle className="cpb-var-icon" />
                            <span>Dynamic workflow variable configured</span>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}
