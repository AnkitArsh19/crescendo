import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneLight, vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { HiCheck, HiOutlineDuplicate } from 'react-icons/hi';
import { useTheme } from '../../components/ThemeContext';

const LANGUAGE_ORDER = ['cURL', 'JavaScript', 'TypeScript', 'Python', 'Java', 'Go', 'C#', 'PHP', 'Ruby', 'Rust'];
const LANGUAGE_SYNTAX = {
    cURL: 'bash', JavaScript: 'javascript', TypeScript: 'typescript', Python: 'python',
    Java: 'java', Go: 'go', 'C#': 'csharp', PHP: 'php', Ruby: 'ruby', Rust: 'rust'
};

function CopyButton({ text }) {
    const [copied, setCopied] = useState(false);
    const copy = async () => {
        await navigator.clipboard?.writeText(text);
        setCopied(true);
        window.setTimeout(() => setCopied(false), 2000);
    };

    return (
        <button className="docs-copy-btn" onClick={copy} aria-label="Copy code">
            {copied ? <HiCheck className="docs-copy-success" /> : <HiOutlineDuplicate />}
        </button>
    );
}

function dereference(spec, schema) {
    if (!schema?.$ref) return schema || {};
    const path = schema.$ref.replace(/^#\//, '').split('/');
    return path.reduce((value, part) => value?.[part], spec) || schema;
}

function exampleForSchema(spec, rawSchema, propertyName = 'value', depth = 0) {
    const schema = dereference(spec, rawSchema);
    if (depth > 3) return '<object>';
    if (schema.example !== undefined) return schema.example;
    if (schema.default !== undefined) return schema.default;
    if (schema.enum?.length) return schema.enum[0];
    if (schema.allOf) return schema.allOf.reduce((result, part) => ({ ...result, ...exampleForSchema(spec, part, propertyName, depth + 1) }), {});
    if (schema.oneOf?.length || schema.anyOf?.length) return exampleForSchema(spec, schema.oneOf?.[0] || schema.anyOf?.[0], propertyName, depth + 1);

    if (schema.type === 'array') return [exampleForSchema(spec, schema.items || {}, propertyName, depth + 1)];
    if (schema.type === 'object' || schema.properties) {
        const required = schema.required || Object.keys(schema.properties || {});
        const entries = Object.entries(schema.properties || {})
            .filter(([name]) => required.includes(name))
            .map(([name, value]) => [name, exampleForSchema(spec, value, name, depth + 1)]);
        return entries.length ? Object.fromEntries(entries) : {};
    }
    if (schema.type === 'integer' || schema.type === 'number') return 0;
    if (schema.type === 'boolean') return false;
    if (schema.format === 'uuid') return `<${propertyName}-uuid>`;
    if (schema.format === 'date-time') return '2026-01-01T00:00:00Z';
    if (schema.format === 'email') return 'user@example.com';
    return `<${propertyName}>`;
}

function jsonExample(spec, operation) {
    const content = operation.requestBody?.content?.['application/json'];
    return content?.schema ? JSON.stringify(exampleForSchema(spec, content.schema), null, 2) : '';
}

function escapeDoubleQuoted(value) {
    return value.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, '\\n');
}

function snippetsFor(method, fullUrl, jsonBody) {
    const methodUpper = method.toUpperCase();
    const hasBody = Boolean(jsonBody) && ['POST', 'PUT', 'PATCH'].includes(methodUpper);
    const curlBody = hasBody ? ` \\\n+  -H "Content-Type: application/json" \\\n+  --data '${jsonBody}'` : '';
    const jsBody = hasBody ? `,
  headers: { authorization: 'Bearer ' + process.env.CRESCENDO_API_KEY, 'content-type': 'application/json' },
  body: JSON.stringify(${jsonBody})` : `,
  headers: { authorization: 'Bearer ' + process.env.CRESCENDO_API_KEY }`;
    const pythonBody = hasBody ? `,
    json=json.loads(r'''${jsonBody}''')` : '';
    const javaBody = hasBody
        ? `.header("Content-Type", "application/json")
    .method("${methodUpper}", HttpRequest.BodyPublishers.ofString("${escapeDoubleQuoted(jsonBody)}"))`
        : `.${methodUpper}()`;
    const goBody = hasBody ? `strings.NewReader(\`${jsonBody}\`)` : 'nil';
    const goImports = hasBody ? '    "strings"\n' : '';
    const csharpBody = hasBody ? `
    Content = new StringContent(${JSON.stringify(jsonBody)}, Encoding.UTF8, "application/json")` : '';
    const phpBody = hasBody ? `
$body = <<<'JSON'
${jsonBody}
JSON;` : '';
    const rustBody = hasBody ? `.header("content-type", "application/json")
        .body(r#"${jsonBody}"#)` : '';

    return {
        cURL: `curl --request ${methodUpper} '${fullUrl}' \\
  --header 'Authorization: Bearer '"$CRESCENDO_API_KEY"${curlBody}`,
        JavaScript: `const response = await fetch('${fullUrl}', {
  method: '${methodUpper}'${jsBody}
});

if (!response.ok) throw new Error(await response.text());
console.log(await response.json());`,
        TypeScript: `const response = await fetch('${fullUrl}', {
  method: '${methodUpper}'${jsBody}
});

if (!response.ok) throw new Error(await response.text());
const data: unknown = await response.json();
console.log(data);`,
        Python: `import json
import os
import requests

response = requests.request(
    '${methodUpper}',
    '${fullUrl}',
    headers={'Authorization': f"Bearer {os.environ['CRESCENDO_API_KEY']}"}${pythonBody},
    timeout=30,
)
response.raise_for_status()
print(response.json())`,
        Java: `import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

var request = HttpRequest.newBuilder()
    .uri(URI.create("${fullUrl}"))
    .header("Authorization", "Bearer " + System.getenv("CRESCENDO_API_KEY"))
    ${javaBody}
    .build();

var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
if (response.statusCode() >= 400) throw new RuntimeException(response.body());
System.out.println(response.body());`,
        Go: `package main

import (
    "fmt"
    "io"
    "net/http"
    "os"
${goImports})

func main() {
    req, err := http.NewRequest("${methodUpper}", "${fullUrl}", ${goBody})
    if err != nil { panic(err) }
    req.Header.Set("Authorization", "Bearer "+os.Getenv("CRESCENDO_API_KEY"))
${hasBody ? '    req.Header.Set("Content-Type", "application/json")\n' : ''}    res, err := http.DefaultClient.Do(req)
    if err != nil { panic(err) }
    defer res.Body.Close()
    body, _ := io.ReadAll(res.Body)
    if res.StatusCode >= 400 { panic(string(body)) }
    fmt.Println(string(body))
}`,
        'C#': `using System.Net.Http.Headers;
using System.Text;

using var client = new HttpClient();
client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue(
    "Bearer", Environment.GetEnvironmentVariable("CRESCENDO_API_KEY"));

using var request = new HttpRequestMessage(HttpMethod.${methodUpper[0] + methodUpper.slice(1).toLowerCase()}, "${fullUrl}") {${csharpBody}
};
using var response = await client.SendAsync(request);
response.EnsureSuccessStatusCode();
Console.WriteLine(await response.Content.ReadAsStringAsync());`,
        PHP: `<?php

require 'vendor/autoload.php';

$client = new GuzzleHttp\\Client();
${phpBody}
$response = $client->request('${methodUpper}', '${fullUrl}', [
    'headers' => ['Authorization' => 'Bearer ' . getenv('CRESCENDO_API_KEY')]${hasBody ? ",\n    'body' => $body" : ''}
]);

echo $response->getBody();`,
        Ruby: `require 'net/http'
require 'uri'

uri = URI('${fullUrl}')
request = Net::HTTP::${methodUpper[0] + methodUpper.slice(1).toLowerCase()}.new(uri)
request['Authorization'] = "Bearer #{ENV.fetch('CRESCENDO_API_KEY')}"${hasBody ? `
request['Content-Type'] = 'application/json'
request.body = ${JSON.stringify(jsonBody)}` : ''}

response = Net::HTTP.start(uri.hostname, uri.port, use_ssl: true) { |http| http.request(request) }
raise response.body unless response.is_a?(Net::HTTPSuccess)
puts response.body`,
        Rust: `use reqwest::Client;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    let response = Client::new()
        .request(reqwest::Method::${methodUpper}, "${fullUrl}")
        .bearer_auth(std::env::var("CRESCENDO_API_KEY")?)
        ${rustBody}
        .send()
        .await?
        .error_for_status()?;

    println!("{}", response.text().await?);
    Ok(())
}`
    };
}

function CodeTabs({ method, path, operation, spec }) {
    const [active, setActive] = useState('cURL');
    const baseUrl = spec.servers?.[0]?.url || 'https://api.crescendo.run';
    const url = `${baseUrl}${path.replace(/\{([^}]+)\}/g, '<$1>')}`;
    const body = jsonExample(spec, operation);
    const snippets = useMemo(() => snippetsFor(method, url, body), [method, url, body]);
    const { theme } = useTheme();

    return (
        <section className="docs-api-code" aria-label="Request examples">
            <div className="docs-language-tabs" role="tablist" aria-label="Code language">
                {LANGUAGE_ORDER.map((language) => (
                    <button key={language} type="button" role="tab" aria-selected={active === language}
                        className={active === language ? 'active' : ''} onClick={() => setActive(language)}>
                        {language}
                    </button>
                ))}
                <CopyButton text={snippets[active]} />
            </div>
            <SyntaxHighlighter style={theme === 'dark' ? vscDarkPlus : oneLight}
                language={LANGUAGE_SYNTAX[active]} PreTag="div" className="docs-api-code-body"
                customStyle={{ margin: 0, background: 'transparent' }}>
                {snippets[active]}
            </SyntaxHighlighter>
        </section>
    );
}

function SchemaFields({ spec, schema }) {
    const resolved = dereference(spec, schema);
    const properties = Object.entries(resolved.properties || {});
    if (!properties.length) return null;
    const required = new Set(resolved.required || []);
    return (
        <div className="docs-api-fields">
            {properties.map(([name, value]) => {
                const field = dereference(spec, value);
                return (
                    <div className="docs-api-field" key={name}>
                        <div><code>{name}</code>{required.has(name) && <span className="docs-required">Required</span>}</div>
                        <span>{field.type || (field.properties ? 'object' : 'value')}{field.format ? ` · ${field.format}` : ''}</span>
                        {field.description && <p>{field.description}</p>}
                    </div>
                );
            })}
        </div>
    );
}

export default function OpenApiRenderer({ targetTag }) {
    const [spec, setSpec] = useState(null);
    const [error, setError] = useState(null);
    const [referenceBaseUrl, setReferenceBaseUrl] = useState(null);
    const [reloadToken, setReloadToken] = useState(0);

    useEffect(() => {
        const controller = new AbortController();
        const configuredBaseUrl = (import.meta.env.VITE_API_URL || 'https://api.crescendo.run').replace(/\/$/, '');
        const isLocalDocumentation = ['localhost', '127.0.0.1', '::1'].includes(window.location.hostname);
        const candidates = [...new Set([
            ...(isLocalDocumentation ? ['http://localhost:8080'] : []),
            configuredBaseUrl,
        ])];

        setSpec(null);
        setError(null);
        setReferenceBaseUrl(null);

        const load = async () => {
            const failures = [];
            for (const baseUrl of candidates) {
                try {
                    const response = await fetch(`${baseUrl}/api-docs/crescendo-public-api-v1`, { signal: controller.signal });
                    if (!response.ok) {
                        failures.push(`${baseUrl} returned ${response.status}`);
                        continue;
                    }
                    const nextSpec = await response.json();
                    if (!nextSpec?.paths) throw new Error('The response was not an OpenAPI document');
                    setSpec(nextSpec);
                    setReferenceBaseUrl(baseUrl);
                    return;
                } catch (requestError) {
                    if (requestError.name === 'AbortError') return;
                    failures.push(`${baseUrl} could not be reached`);
                }
            }
            setError(failures.join('. '));
        };

        load();
        return () => controller.abort();
    }, [reloadToken]);

    if (error) return <div className="docs-status docs-error"><p className="docs-eyebrow">Live reference unavailable</p><h1>We could not load the API contract</h1><p>{error}.</p><p>For local development, start the backend on port 8080 or set <code>VITE_API_URL</code> to its reachable address. For production, the API deployment must serve this public OpenAPI endpoint.</p><button type="button" className="docs-retry-button" onClick={() => setReloadToken((current) => current + 1)}>Try again</button></div>;
    if (!spec) return <div className="docs-status">Loading the live API reference…</div>;

    const endpoints = Object.entries(spec.paths || {}).flatMap(([path, methods]) =>
        Object.entries(methods)
            .filter(([, operation]) => operation.tags?.includes(targetTag))
            .map(([method, operation]) => ({ path, method, operation })));

    if (!endpoints.length) return <div className="docs-status"><h1>{targetTag}</h1><p>No public endpoints are currently published for this group.</p></div>;

    return (
        <motion.div className="docs-api-container" initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.25 }}>
            <p className="docs-eyebrow">Live public contract</p>
            <h1>{targetTag} API</h1>
            <p className="docs-api-intro">These endpoints are generated from Crescendo’s OpenAPI contract. Authenticate server-side with a Bearer API key and use the scopes listed in each endpoint description.</p>
            <div className="docs-api-base-url"><span>API base URL</span><code>{spec.servers?.[0]?.url || referenceBaseUrl || 'https://api.crescendo.run'}</code><span className="docs-api-source">Reference loaded from {referenceBaseUrl}</span></div>

            {endpoints.map(({ path, method, operation }) => {
                const requestSchema = operation.requestBody?.content?.['application/json']?.schema;
                return (
                    <article key={`${method}-${path}`} className="docs-api-endpoint-card">
                        <div className="docs-api-endpoint"><span className={`docs-api-method ${method}`}>{method}</span><code>{path}</code></div>
                        <h2>{operation.summary || `${method.toUpperCase()} ${path}`}</h2>
                        <p>{operation.description || 'Call this endpoint from a trusted server after granting its required API key scope.'}</p>
                        {operation.parameters?.length > 0 && <section><h3>Parameters</h3><div className="docs-api-fields">{operation.parameters.map((parameter) => <div className="docs-api-field" key={`${parameter.in}-${parameter.name}`}><div><code>{parameter.name}</code>{parameter.required && <span className="docs-required">Required</span>}</div><span>{parameter.in} · {dereference(spec, parameter.schema).type || 'value'}</span>{parameter.description && <p>{parameter.description}</p>}</div>)}</div></section>}
                        {requestSchema && <section><h3>Request body</h3><p>Replace placeholder values with data from your application. The example includes required fields from the current schema.</p><SchemaFields spec={spec} schema={requestSchema} /></section>}
                        <section><h3>Example request</h3><p>Each language tab makes the same request. Parameters and request fields are documented once above; choose the tab that matches your server.</p><CodeTabs method={method} path={path} operation={operation} spec={spec} /></section>
                        {operation.responses && <section><h3>Responses</h3><div className="docs-api-responses">{Object.entries(operation.responses).map(([status, response]) => <div key={status}><code>{status}</code><span>{response.description || 'Response documented in the OpenAPI contract.'}</span></div>)}</div></section>}
                    </article>
                );
            })}
        </motion.div>
    );
}
