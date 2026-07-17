import { useState } from 'react';
import { motion } from 'framer-motion';
import { HiOutlineCheck } from 'react-icons/hi';
import './ApiSection.css';

const apiFeatures = [
    { text: <><strong>Public REST API</strong> - Work with email, domains, contacts, suppressions, workflows, connections, runs, and the app catalog</> },
    { text: <><strong>Idempotent writes</strong> - POST responses are protected with an Idempotency-Key for safe retries</> },
    { text: <><strong>Cursor pagination</strong> - List resources through a stable data, has_more, and next_cursor envelope</> },
    { text: <><strong>Eight official SDKs</strong> - Hand-written Node.js and Python clients plus generated Java, Go, Rust, PHP, Ruby, and .NET libraries</> },
];

const sdkExamples = [
    {
        id: 'node', label: 'Node.js', filename: 'send-email.js',
        code: `// npm install @crescendo/email\nimport { crescendo } from '@crescendo/email';\n\nconst client = crescendo({ apiKey: process.env.CRESCENDO_API_KEY });\nawait client.emails.send({\n  from: 'hello@yourdomain.com',\n  to: 'customer@example.com',\n  subject: 'Welcome to Crescendo',\n  htmlBody: '<h1>Welcome</h1>',\n  textBody: 'Welcome',\n  emailType: 'TRANSACTIONAL',\n});`,
    },
    {
        id: 'python', label: 'Python', filename: 'send_email.py',
        code: `# pip install crescendo\nfrom crescendo import Crescendo\n\nclient = Crescendo(api_key=os.environ['CRESCENDO_API_KEY'])\nclient.emails.send(\n    from_address='hello@yourdomain.com',\n    to='customer@example.com',\n    subject='Welcome to Crescendo',\n    html_body='<h1>Welcome</h1>',\n    text_body='Welcome',\n)`,
    },
    {
        id: 'java', label: 'Java', filename: 'SendEmail.java',
        code: `import io.crescendo.ApiClient;\nimport io.crescendo.Configuration;\nimport io.crescendo.api.EmailsApi;\nimport io.crescendo.model.SendEmailRequest;\n\nApiClient client = Configuration.getDefaultApiClient();\nEmailsApi emails = new EmailsApi(client);\nSendEmailRequest request = new SendEmailRequest()\n    .from("hello@yourdomain.com")\n    .to("customer@example.com")\n    .subject("Welcome to Crescendo")\n    .emailType("TRANSACTIONAL");\nemails.sendEmail(request);`,
    },
    {
        id: 'go', label: 'Go', filename: 'send_email.go',
        code: `import (\n  "context"\n  crescendo "github.com/AnkitArsh19/crescendo-go"\n)\n\nclient := crescendo.NewAPIClient(crescendo.NewConfiguration())\nauth := context.WithValue(context.Background(),\n  crescendo.ContextAccessToken, os.Getenv("CRESCENDO_API_KEY"))\nrequest := crescendo.NewSendEmailRequest(\n  "hello@yourdomain.com", "customer@example.com",\n  "Welcome to Crescendo", "TRANSACTIONAL")\n_, _, err := client.EmailsAPI.SendEmail(auth).\n  SendEmailRequest(*request).Execute()`,
    },
    {
        id: 'php', label: 'PHP', filename: 'send-email.php',
        code: `<?php\n$configuration = Crescendo\\Configuration::getDefaultConfiguration();\n$configuration->setApiKey('ApiKeyAuth', getenv('CRESCENDO_API_KEY'));\n$emails = new Crescendo\\Api\\EmailsApi(null, $configuration);\n\n$request = new Crescendo\\Model\\SendEmailRequest();\n$request->setFrom('hello@yourdomain.com');\n$request->setTo('customer@example.com');\n$request->setSubject('Welcome to Crescendo');\n$request->setEmailType('TRANSACTIONAL');\n$emails->sendEmail($request);`,
    },
    {
        id: 'ruby', label: 'Ruby', filename: 'send_email.rb',
        code: `require 'crescendo'\n\nCrescendo.configure do |config|\n  config.api_key['ApiKeyAuth'] = ENV.fetch('CRESCENDO_API_KEY')\nend\n\nemails = Crescendo::EmailsApi.new\nrequest = Crescendo::SendEmailRequest.new(\n  from: 'hello@yourdomain.com', to: 'customer@example.com',\n  subject: 'Welcome to Crescendo', email_type: 'TRANSACTIONAL'\n)\nemails.send_email(request)`,
    },
    {
        id: 'rust', label: 'Rust', filename: 'send_email.rs',
        code: `use crescendo::{apis, models};\n\nlet mut config = apis::configuration::Configuration::new();\nconfig.bearer_access_token = Some(std::env::var("CRESCENDO_API_KEY")?);\nlet request = models::SendEmailRequest {\n    from: "hello@yourdomain.com".into(),\n    to: "customer@example.com".into(),\n    subject: "Welcome to Crescendo".into(),\n    email_type: "TRANSACTIONAL".into(),\n    ..Default::default()\n};\napis::emails_api::send_email(&config, request).await?;`,
    },
    {
        id: 'dotnet', label: '.NET', filename: 'SendEmail.cs',
        code: `using Crescendo.Api;\nusing Crescendo.Client;\nusing Crescendo.Model;\n\nvar token = new BearerToken(Environment\n    .GetEnvironmentVariable("CRESCENDO_API_KEY"));\nvar emails = new EmailsApi(httpClient, tokenProvider);\nvar request = new SendEmailRequest(\n    from: "hello@yourdomain.com", to: "customer@example.com",\n    subject: "Welcome to Crescendo", emailType: "TRANSACTIONAL");\nawait emails.SendEmailAsync(request);`,
    },
];

export default function ApiSection() {
    const [selectedSdk, setSelectedSdk] = useState('node');
    const example = sdkExamples.find((item) => item.id === selectedSdk) || sdkExamples[0];

    return (
        <section className="api-section" id="api">
            <div className="api-inner">
                <motion.div className="api-content" initial={{ opacity: 0, x: -30 }} whileInView={{ opacity: 1, x: 0 }} viewport={{ once: true, margin: '-50px' }} transition={{ duration: 0.7, ease: [0.22, 1, 0.36, 1] }}>
                    <p className="section-label">Public API</p>
                    <h2 className="section-title">Built for <span className="font-serif" style={{ fontStyle: 'italic' }}>developers</span></h2>
                    <p className="section-subtitle">Use the public API for CrescendoMail and automation resources, backed by scoped API keys, consistent errors, idempotent writes, and generated clients.</p>
                    <div className="api-features">
                        {apiFeatures.map((feature, index) => (
                            <motion.div className="api-feature" key={index} initial={{ opacity: 0, x: -20 }} whileInView={{ opacity: 1, x: 0 }} viewport={{ once: true }} transition={{ delay: 0.2 + index * 0.1, duration: 0.5 }}>
                                <div className="api-feature-icon"><HiOutlineCheck /></div>
                                <div className="api-feature-text">{feature.text}</div>
                            </motion.div>
                        ))}
                    </div>
                </motion.div>

                <motion.div className="api-code-wrapper" initial={{ opacity: 0, x: 30, rotateY: 5 }} whileInView={{ opacity: 1, x: 0, rotateY: 0 }} viewport={{ once: true, margin: '-50px' }} transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}>
                    <div className="api-code-glow" />
                    <div className="api-code-block">
                        <div className="api-code-header">
                            <div className="api-code-dots"><span className="api-code-dot" /><span className="api-code-dot" /><span className="api-code-dot" /></div>
                            <span className="api-code-filename">{example.filename}</span>
                        </div>
                        <div className="api-code-tabs" role="tablist" aria-label="Crescendo SDK languages">
                            {sdkExamples.map((sdk) => <button key={sdk.id} type="button" role="tab" aria-selected={selectedSdk === sdk.id} className={selectedSdk === sdk.id ? 'active' : ''} onClick={() => setSelectedSdk(sdk.id)}>{sdk.label}</button>)}
                        </div>
                        <div className="api-code-body" role="tabpanel" aria-label={`${example.label} example`}><pre>{example.code}</pre></div>
                    </div>
                </motion.div>
            </div>
        </section>
    );
}
