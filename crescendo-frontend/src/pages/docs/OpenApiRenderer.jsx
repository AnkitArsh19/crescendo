import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { HiOutlineDuplicate, HiCheck } from 'react-icons/hi';

function CopyButton({ text }) {
    const [copied, setCopied] = useState(false);
    
    const handleCopy = () => {
        navigator.clipboard.writeText(text);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    };

    return (
        <button className="docs-copy-btn" onClick={handleCopy} aria-label="Copy code">
            {copied ? <HiCheck className="text-green-400" /> : <HiOutlineDuplicate />}
        </button>
    );
}

function CodeTabs({ method, path, operation }) {
    const [activeTab, setActiveTab] = useState('cURL');
    
    const baseUrl = 'https://api.crescendo.run';
    const fullUrl = `${baseUrl}${path}`;
    const uppercaseMethod = method.toUpperCase();
    
    // Auto-generate a dummy JSON payload based on request body if available
    let jsonPayload = '{}';
    if (operation.requestBody?.content?.['application/json']?.schema) {
        // Just a simple placeholder to show there's a body
        jsonPayload = '{\n  "key": "value"\n}';
    }
    const hasBody = ['POST', 'PUT', 'PATCH'].includes(uppercaseMethod);

    const snippets = {
        'cURL': `curl -X ${uppercaseMethod} "${fullUrl}" \\
  -H "Authorization: Bearer <API_KEY>"${hasBody ? ` \\\n  -H "Content-Type: application/json" \\\n  -d '${jsonPayload}'` : ''}`,
        
        'Python': `import requests

url = "${fullUrl}"
headers = {
    "Authorization": "Bearer <API_KEY>"${hasBody ? `,\n    "Content-Type": "application/json"` : ''}
}
${hasBody ? `data = ${jsonPayload}\n\nresponse = requests.${method.toLowerCase()}(url, headers=headers, json=data)` : `\nresponse = requests.${method.toLowerCase()}(url, headers=headers)`}
print(response.json())`,

        'Node.js': `const fetch = require('node-fetch');

const url = '${fullUrl}';
const options = {
  method: '${uppercaseMethod}',
  headers: {
    'Authorization': 'Bearer <API_KEY>'${hasBody ? `,\n    'Content-Type': 'application/json'` : ''}
  }${hasBody ? `,\n  body: JSON.stringify(${jsonPayload})` : ''}
};

fetch(url, options)
  .then(res => res.json())
  .then(json => console.log(json))
  .catch(err => console.error('error:' + err));`,

        'Go': `package main

import (
\t"fmt"
\t"net/http"
\t"io"
${hasBody ? '\t"strings"\n' : ''})

func main() {
\turl := "${fullUrl}"
${hasBody ? `\tpayload := strings.NewReader(\`${jsonPayload}\`)\n` : ''}
\treq, _ := http.NewRequest("${uppercaseMethod}", url, ${hasBody ? 'payload' : 'nil'})
\treq.Header.Add("Authorization", "Bearer <API_KEY>")
${hasBody ? '\treq.Header.Add("Content-Type", "application/json")\n' : ''}
\tres, _ := http.DefaultClient.Do(req)
\tdefer res.Body.Close()

\tbody, _ := io.ReadAll(res.Body)
\tfmt.Println(string(body))
}`,

        'Java': `import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("${fullUrl}"))
            .header("Authorization", "Bearer <API_KEY>")
            ${hasBody ? `.header("Content-Type", "application/json")\n            .method("${uppercaseMethod}", HttpRequest.BodyPublishers.ofString("${jsonPayload.replace(/\n/g, '').replace(/"/g, '\\"')}"))` : `.${uppercaseMethod}()`}
            .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}`,

        'Rust': `use reqwest;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let client = reqwest::Client::new();
    let res = client.${method.toLowerCase()}("${fullUrl}")
        .header("Authorization", "Bearer <API_KEY>")
        ${hasBody ? `.header("Content-Type", "application/json")\n        .body(r#"${jsonPayload}"#)` : ''}
        .send()
        .await?;

    let text = res.text().await?;
    println!("{text}");
    Ok(())
}`
    };

    return (
        <div className="docs-code-container">
            <div className="docs-code-header" style={{ display: 'flex', gap: '1rem', overflowX: 'auto', background: 'var(--bg-elevated)', borderBottom: '1px solid var(--border-primary)' }}>
                <div style={{ display: 'flex', gap: '0.5rem', flexGrow: 1 }}>
                    {Object.keys(snippets).map(lang => (
                        <button 
                            key={lang}
                            onClick={() => setActiveTab(lang)}
                            style={{ 
                                background: activeTab === lang ? 'var(--bg-card)' : 'transparent',
                                border: 'none',
                                color: activeTab === lang ? 'var(--text-primary)' : 'var(--text-tertiary)',
                                padding: '0.5rem 1rem',
                                cursor: 'pointer',
                                fontSize: '0.85rem',
                                borderTopLeftRadius: '6px',
                                borderTopRightRadius: '6px',
                                fontWeight: activeTab === lang ? '600' : '400',
                                borderTop: activeTab === lang ? '1px solid var(--border-primary)' : 'none',
                                borderLeft: activeTab === lang ? '1px solid var(--border-primary)' : 'none',
                                borderRight: activeTab === lang ? '1px solid var(--border-primary)' : 'none',
                                marginBottom: activeTab === lang ? '-1px' : '0'
                            }}
                        >
                            {lang}
                        </button>
                    ))}
                </div>
                <CopyButton text={snippets[activeTab]} />
            </div>
            <SyntaxHighlighter
                style={vscDarkPlus}
                language={activeTab.toLowerCase().replace('.js', '').replace('node.js', 'javascript')}
                PreTag="div"
                customStyle={{ margin: 0, borderRadius: '0 0 8px 8px', background: 'var(--bg-card)', border: 'none' }}
            >
                {snippets[activeTab]}
            </SyntaxHighlighter>
        </div>
    );
}

export default function OpenApiRenderer({ targetTag }) {
    const [spec, setSpec] = useState(null);
    const [error, setError] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const baseUrl = import.meta.env.VITE_API_URL || 'https://api.crescendo.run';
        fetch(`${baseUrl}/api-docs`)
            .then(res => {
                if (!res.ok) throw new Error('Failed to fetch OpenAPI spec');
                return res.json();
            })
            .then(data => {
                setSpec(data);
                setLoading(false);
            })
            .catch(err => {
                console.error(err);
                setError(err.message);
                setLoading(false);
            });
    }, []);

    if (loading) return <div className="docs-loading">Loading API Spec...</div>;
    if (error) return <div className="docs-error">Error loading API Spec: {error}</div>;
    if (!spec) return null;

    const endpoints = [];
    Object.keys(spec.paths).forEach(path => {
        Object.keys(spec.paths[path]).forEach(method => {
            const operation = spec.paths[path][method];
            if (operation.tags && operation.tags.includes(targetTag)) {
                endpoints.push({ path, method, operation });
            }
        });
    });

    if (endpoints.length === 0) {
        return (
            <div className="docs-api-empty">
                <h2>{targetTag} API</h2>
                <p>No endpoints documented for this tag yet.</p>
            </div>
        );
    }

    return (
        <motion.div 
            className="docs-api-container"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.3 }}
        >
            <h1>{targetTag} API Reference</h1>
            <p>Base URL: <code>https://api.crescendo.run</code></p>
            
            {endpoints.map((ep, idx) => (
                <div key={idx} className="docs-api-endpoint-card" style={{ marginBottom: '4rem' }}>
                    <div className="docs-api-endpoint" style={{ padding: '1rem', border: '1px solid var(--border-secondary)', borderRadius: '8px', background: 'var(--bg-card)' }}>
                        <span className={`docs-api-method ${ep.method}`}>{ep.method}</span>
                        <span className="docs-api-path">{ep.path}</span>
                    </div>
                    
                    <h3 className="docs-api-summary" style={{ marginTop: '1.5rem', marginBottom: '0.5rem' }}>{ep.operation.summary}</h3>
                    <p className="docs-api-description" style={{ color: 'var(--text-secondary)' }}>{ep.operation.description}</p>
                    
                    <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap', marginTop: '2rem' }}>
                        <div style={{ flex: '1 1 300px' }}>
                            {ep.operation.parameters && ep.operation.parameters.length > 0 && (
                                <div className="docs-api-params">
                                    <h4 style={{ borderBottom: '1px solid var(--border-primary)', paddingBottom: '0.5rem', marginBottom: '1rem' }}>Parameters</h4>
                                    <ul style={{ listStyle: 'none', padding: 0 }}>
                                        {ep.operation.parameters.map((param, pIdx) => (
                                            <li key={pIdx} style={{ marginBottom: '1rem', padding: '1rem', background: 'var(--bg-card)', border: '1px solid var(--border-primary)', borderRadius: '6px' }}>
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                                                    <strong style={{ fontFamily: 'monospace', color: 'var(--text-primary)' }}>{param.name}</strong> 
                                                    <span style={{ fontSize: '0.8rem', color: 'var(--text-tertiary)' }}>in {param.in}</span>
                                                    {param.required && <span style={{ fontSize: '0.75rem', background: 'rgba(245,158,11,0.1)', color: '#f59e0b', padding: '0.1rem 0.4rem', borderRadius: '4px', border: '1px solid rgba(245,158,11,0.2)' }}>Required</span>}
                                                </div>
                                                <p style={{ margin: 0, fontSize: '0.9rem', color: 'var(--text-secondary)' }}>{param.description}</p>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                        </div>
                        <div style={{ flex: '1 1 400px' }}>
                            <h4 style={{ borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem', marginBottom: '1rem' }}>Example Request</h4>
                            <CodeTabs method={ep.method} path={ep.path} operation={ep.operation} />
                        </div>
                    </div>
                </div>
            ))}
        </motion.div>
    );
}
