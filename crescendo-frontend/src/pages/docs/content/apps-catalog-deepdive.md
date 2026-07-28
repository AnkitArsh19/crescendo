# Deep Dive App Catalog (Backend Integrations Reference)

This comprehensive reference manual details the native backend app modules implemented in `com.crescendo.apps`. Each entry provides step-by-step provider developer setup instructions, required OAuth permissions and scopes, supported triggers and actions, configuration parameters, and dynamic variable interpolation examples.

---

## Navigation & Cross-References
* [Connections Architecture & Security (BYOK & OAuth)](/docs/byok-vs-oauth)
* [Workflow Studio & Canvas Manual](/docs/workflow-canvas)
* [Developer REST API Reference](/docs/public-api)
* [Node.js & TypeScript SDK Reference](/docs/sdk-node)
* [Python SDK Reference](/docs/sdk-python)

---

## 1. Primary Productivity & Communication Integrations

### Gmail (`gmail`)

Google Gmail is an enterprise messaging platform used by millions of organizations. The Crescendo `gmail` integration allows you to automate inbox processing, send HTML emails on behalf of authenticated accounts, search messages, and manage labels.

#### OAuth 2.0 Permissions & Scope Setup
To connect Gmail using your own Custom OAuth App (or Crescendo Managed OAuth):
1. Navigate to the **Google Cloud Console** (`console.cloud.google.com`).
2. Create a project and enable the **Gmail API** under **APIs & Services > Library**.
3. Configure the **OAuth Consent Screen** and add the following required authorization scopes:
   * `https://mail.google.com/` (Full access to send, read, and manage labels)
   * `https://www.googleapis.com/auth/gmail.send` (Send emails on user behalf)
   * `https://www.googleapis.com/auth/gmail.labels` (Manage custom inbox labels)
4. Under **Credentials**, create an **OAuth 2.0 Client ID** (Application Type: *Web application*).
5. Set the **Authorized Redirect URI** to:
   `https://api.crescendo.run/api/v1/connections/oauth/callback`
6. Copy the generated **Client ID** and **Client Secret** into your Crescendo connection setup.

#### Triggers

##### `new-email` (New Email Received)
Triggers an automated workflow run whenever a new incoming message hits your Gmail inbox matching specified search criteria.
* **Config Schema Parameters:**
  * `labelFilter` (type: `dynamic_dropdown`, required: `false`): Filter messages by label (e.g., `INBOX`, `IMPORTANT`, `UNREAD`).
  * `subjectFilter` (type: `text`, required: `false`, placeholder: `Invoice`): Keyword string that must be present in the email subject line.
  * `fromFilter` (type: `text`, required: `false`, placeholder: `billing@vendor.com`): Email address or domain that the incoming message must originate from.
* **Output Payload Variables:**
  * `{{steps.trigger.messageId}}` - Unique internal Gmail message identifier.
  * `{{steps.trigger.threadId}}` - Thread conversation ID.
  * `{{steps.trigger.from}}` - Sender email address.
  * `{{steps.trigger.subject}}` - Email subject line text.
  * `{{steps.trigger.body}}` - Plain text or HTML body content.

#### Actions

##### `send` (Send Email)
Dispatches an email message directly from the authenticated user's Gmail account.
* **Config Schema Parameters:**
  * `to` (type: `text`, required: `true`, placeholder: `recipient@example.com`): Target destination email address.
  * `cc` (type: `text`, required: `false`): Optional CC recipient addresses.
  * `bcc` (type: `text`, required: `false`): Optional BCC recipient addresses.
  * `subject` (type: `text`, required: `true`, placeholder: `Monthly Status Update`): Subject line text.
  * `body` (type: `textarea`, required: `true`): Email body content (HTML tags supported).
  * `threadId` (type: `text`, required: `false`): Pass an existing thread ID to send the email as a reply in an existing conversation thread.
* **Output Return Data:**
  * `{{steps.step_order.id}}` - Dispatched message ID.
  * `{{steps.step_order.threadId}}` - Thread conversation ID.

##### `addLabel` (Add Label to Message)
Applies a Gmail label to organize an incoming or existing email.
* **Config Schema Parameters:**
  * `messageId` (type: `text`, required: `true`): Message ID to label (e.g., `{{steps.trigger.messageId}}`).
  * `labelId` (type: `dynamic_dropdown`, required: `true`): Target label selected from your account.

---

### Slack (`slack`)

Slack is a collaboration and team chat platform. The `slack` integration allows you to post messages, notify channels, monitor mentions, and trigger workflows on emoji reactions.

#### OAuth & Bot Token Permissions Setup
1. Open the **Slack App Directory** (`api.slack.com/apps`).
2. Create a new Slack App within your target workspace.
3. Under **OAuth & Permissions**, add the following **Bot Token Scopes**:
   * `chat:write` (Post channel messages and direct messages)
   * `channels:read` (List public channels in workspace)
   * `channels:manage` (Create and edit channel details)
   * `reactions:read` (Listen for emoji reaction triggers)
   * `users:read` (Retrieve workspace team user profiles)
4. Under **OAuth & Permissions**, set the **Redirect URL** to:
   `https://api.crescendo.run/api/v1/connections/oauth/callback`
5. Install the App to your workspace and copy either your **Bot User OAuth Token** (`xoxb-...`) for BYOK setups, or use your **Client ID** and **Client Secret** for managed OAuth.

#### Triggers

##### `new-message` (New Message in Channel)
Triggers when a message is posted to a specific channel.
* **Config Schema Parameters:**
  * `channel` (type: `dynamic_dropdown`, required: `true`): Select target channel.

##### `new-mention` (New Bot Mention)
Triggers when your bot user or specified keyword is mentioned in any accessible channel.

##### `new-reaction` (New Emoji Reaction Added)
Triggers when a user adds an emoji reaction to a channel message.
* **Config Schema Parameters:**
  * `channel` (type: `dynamic_dropdown`, required: `true`): Target channel.
  * `emojiFilter` (type: `text`, required: `false`, placeholder: `thumbsup`): Filter by specific emoji string.

#### Actions

##### `sendMessage` (Send Channel Message)
Posts a message to a Slack channel.
* **Config Schema Parameters:**
  * `channel` (type: `dynamic_dropdown`, required: `true`): Destination channel.
  * `text` (type: `textarea`, required: `true`, placeholder: `System Alert: High CPU Load`): Message text (Markdown/mrkdwn formatting supported).
  * `threadTs` (type: `text`, required: `false`): Parent message timestamp to post as a threaded reply.

---

### GitHub (`github`)

GitHub provides software repository hosting, version control, issue tracking, and automated release workflows.

#### Token & OAuth Setup
1. Go to **GitHub Settings > Developer Settings > Personal Access Tokens** or **OAuth Apps**.
2. Select tokens or OAuth application credentials with the following scopes:
   * `repo` (Full control over private and public repositories)
   * `workflow` (Manage GitHub Actions workflows)
   * `admin:repo_hook` (Manage repository webhooks)
3. For OAuth setups, set the Redirect URI to `https://api.crescendo.run/api/v1/connections/oauth/callback`.

#### Triggers
* `push` (Push Event): Triggers when commits are pushed to a specified repository branch.
* `pull_request` (Pull Request Event): Triggers when a PR is opened, labeled, or merged.
* `issues` (Issue Event): Triggers when a new repository issue is created or updated.

#### Actions
* `createIssue` (Create Repository Issue): Opens a new issue ticket.
  * `owner` (required: `true`): Repository owner username or organization name.
  * `repo` (required: `true`): Target repository name.
  * `title` (required: `true`): Issue title.
  * `body` (required: `false`): Markdown description text.
  * `labels` (required: `false`): Comma-separated labels (e.g., `bug, priority-high`).

---

## 2. Comprehensive Directory of All 113 Native Backend App Modules

Every module below represents an active Java integration package under `com.crescendo.apps`:

| App Key | Display Name | Auth Method | Primary Capabilities |
| :--- | :--- | :--- | :--- |
| `airtable` | Airtable | OAuth 2.0 | Read and write records in Airtable bases |
| `approval` | Human Approval | None | Pause workflow execution awaiting manual human decision |
| `asana` | Asana | OAuth 2.0 | Create tasks and update project milestones |
| `awss3` | AWS S3 | AWS Credentials | Upload, download, and list objects in S3 buckets |
| `brandfetch` | Brandfetch | API Key | Retrieve corporate logos, colors, and domain brand metadata |
| `brevo` | Brevo (Sendinblue) | API Key | Send email marketing campaigns and manage contact lists |
| `calcom` | Cal.com | API Key | Handle scheduling webhooks and booking creation |
| `calendly` | Calendly | OAuth 2.0 | Receive automated meeting booking triggers |
| `catfacts` | Cat Facts | None | Utility generator for random cat trivia facts |
| `clickup` | ClickUp | OAuth 2.0 | Create workspace tasks, folders, and status updates |
| `coingecko` | CoinGecko | None | Retrieve cryptocurrency exchange rates and market data |
| `compression` | Compression | None | Compress or extract ZIP and GZIP archive files |
| `crescendomail` | Crescendo Mail | API Key | Native Crescendo transactional email engine |
| `crypto` | Crypto Utilities | None | Compute cryptographic hashes (SHA-256, MD5, HMAC) |
| `datetime` | Date Time Utility | None | Format, parse, and manipulate ISO date timestamps |
| `discord` | Discord | Webhook / Bot Token | Post channel embeds and moderate community chat |
| `dropbox` | Dropbox | OAuth 2.0 | Upload files and synchronize cloud storage directories |
| `errorhandling` | Error Handler | None | Catch step errors and route fallback execution branches |
| `facebookgraph` | Facebook Graph | OAuth 2.0 | Manage page posts and query Graph API endpoints |
| `figma` | Figma | OAuth 2.0 | Listen for file comment notifications and export assets |
| `freshdesk` | Freshdesk | API Key | Create support tickets and update customer status |
| `ftpsftp` | FTP / SFTP | Basic / SSH Key | Transfer files securely over FTP/SFTP servers |
| `gemini` | Google Gemini | API Key (BYOK) | Multimodal text and image reasoning (`gemini-2.0-flash`) |
| `giphy` | Giphy | API Key | Search and retrieve GIF animation URLs |
| `git` | Git Utilities | SSH / HTTPS | Clone repositories and execute native Git operations |
| `github` | GitHub | OAuth / PAT | Manage repository issues, PRs, releases, and commits |
| `githubstats` | GitHub Stats | API Key | Generate repository activity and contributor stats |
| `gitlab` | GitLab | Personal Token | Manage GitLab pipelines, merge requests, and issues |
| `gmail` | Gmail | OAuth 2.0 | Read incoming mail, send HTML emails, manage labels |
| `googlecalendar`| Google Calendar | OAuth 2.0 | Create events, check availability, and invite guests |
| `googledocs` | Google Docs | OAuth 2.0 | Generate documents, replace text, and append paragraphs |
| `googledrive` | Google Drive | OAuth 2.0 | Upload files, manage permissions, search shared folders |
| `googleforms` | Google Forms | OAuth 2.0 | Listen for new form submission triggers |
| `googlesheets` | Google Sheets | OAuth 2.0 | Append rows, update cells, and query spreadsheet data |
| `googleslides` | Google Slides | OAuth 2.0 | Create presentations and replace slide variables |
| `googletasks` | Google Tasks | OAuth 2.0 | Create and complete personal tasks |
| `googletranslate`| Google Translate| API Key | Translate text across international languages |
| `gotify` | Gotify | App Token | Send push notifications to self-hosted Gotify servers |
| `graphql` | GraphQL Client | Bearer / Custom | Execute raw GraphQL queries and mutation operations |
| `hackernews` | Hacker News | None | Fetch top stories and comment threads |
| `homeassistant` | Home Assistant | Long-Lived Token | Control smart home devices and trigger automations |
| `html` | HTML Processor | None | Render and sanitize HTML strings |
| `htmlextract` | HTML Extractor | None | Parse HTML documents using CSS selector queries |
| `http` | HTTP Request | Bearer / Basic / Header| Execute arbitrary HTTP GET/POST/PUT/PATCH/DELETE calls |
| `hubspot` | HubSpot CRM | OAuth 2.0 | Manage CRM contacts, deals, and sales pipelines |
| `icalendar` | iCalendar | None | Parse and generate `.ics` calendar files |
| `imap` | IMAP Email | Basic Auth | Read incoming messages from custom IMAP mail servers |
| `instagram` | Instagram | OAuth 2.0 | Post images and monitor account comments |
| `jenkins` | Jenkins CI | Basic Auth | Trigger remote Jenkins build jobs |
| `jira` | Jira | OAuth / API Token | Manage software bug tickets and sprint transitions |
| `jobsearch` | Job Search | API Key | Search public employment postings |
| `jokeapi` | Joke API | None | Utility generator for programming jokes |
| `json` | JSON Processor | None | Parse, stringify, and query JSON using JSONPath |
| `jwt` | JWT Utility | None | Sign and verify JSON Web Tokens |
| `kafka` | Apache Kafka | SASL / SSL | Publish and consume events on Kafka topics |
| `leetcode` | LeetCode | None | Fetch user submission statistics and daily problems |
| `linear` | Linear | API Key | Create engineering issues and track cycles |
| `linkedin` | LinkedIn | OAuth 2.0 | Post company updates and monitor interactions |
| `log` | Logger | None | Output debug logs to the Crescendo execution console |
| `logic` | Logic Router | None | Evaluate IF/ELSE conditions and SWITCH branch routes |
| `mailchimp` | Mailchimp | OAuth 2.0 / API Key | Manage newsletter campaigns and subscriber lists |
| `markdown` | Markdown | None | Convert Markdown text to HTML |
| `marketstack` | Marketstack | API Key | Fetch stock market historical prices |
| `matrix` | Matrix | Access Token | Send chat messages to Matrix decentralized rooms |
| `mattermost` | Mattermost | Bot Token | Post messages to open-source Mattermost servers |
| `medium` | Medium | Integration Token | Publish story posts to Medium accounts |
| `microsoftexcel`| Microsoft Excel | OAuth 2.0 | Read and update workbook tables on OneDrive |
| `microsoftoutlook`| MS Outlook | OAuth 2.0 | Send emails and read Outlook calendar events |
| `microsoftteams`| MS Teams | OAuth 2.0 | Send adaptive cards to Teams channels |
| `mongodb` | MongoDB | Connection URI | Query documents and execute collection inserts |
| `mqtt` | MQTT | Basic / TLS | Publish and subscribe to IoT MQTT broker topics |
| `mysql` | MySQL | JDBC Connection | Execute SQL queries against MySQL databases |
| `nasa` | NASA API | API Key | Fetch Astronomy Picture of the Day and satellite data |
| `nativeform` | Native Form | None | Render interactive web forms to collect user inputs |
| `notion` | Notion | OAuth / Integration | Query databases and create workspace pages |
| `openai` | OpenAI | API Key (BYOK) | Generate text (`gpt-4o`) and images (`dall-e-3`) |
| `paypal` | PayPal | Client ID/Secret | Handle payment webhooks and verify transactions |
| `pomodoro` | Pomodoro Timer | None | Utility timer for focus intervals |
| `postgres` | PostgreSQL (CQRS) | JDBC Connection | Core database query execution |
| `postgresql` | PostgreSQL | JDBC Connection | Query external PostgreSQL databases |
| `pushbullet` | Pushbullet | Access Token | Send push notifications to mobile devices |
| `quotes` | Quotes Generator| None | Fetch inspirational quote strings |
| `rabbitmq` | RabbitMQ | Basic / AMQP | Publish messages to RabbitMQ exchanges |
| `razorpay` | Razorpay | Key ID / Secret | Process payments and generate payment orders |
| `readpdf` | PDF Reader | None | Extract text content from PDF documents |
| `redis` | Redis Cache | Connection URI | Execute GET, SET, and DEL key-value operations |
| `renamekeys` | Key Renamer | None | Transform dictionary key names in JSON payloads |
| `rss` | RSS Feed Reader | None | Parse RSS and Atom XML feeds |
| `salesforce` | Salesforce | OAuth 2.0 | Manage CRM leads, accounts, and opportunities |
| `sarvam` | Sarvam AI | API Key (BYOK) | Translate text and convert speech to regional text |
| `schedule` | Scheduler | None | Trigger workflows on cron or interval schedules |
| `set` | Set Variable | None | Store temporary variables in workflow execution context |
| `simpleapi` | Simple API | API Key | Generic REST communication module |
| `slack` | Slack | OAuth / Bot Token | Post channel messages and listen for events |
| `smtp` | SMTP Email | Basic Auth | Send emails via custom SMTP mail servers |
| `spotify` | Spotify | OAuth 2.0 | Manage playlists and control active playback |
| `spreadsheetfile`| Spreadsheet Reader| None | Parse uploaded `.xlsx` and `.csv` files |
| `ssh` | SSH Remote Exec | Password / Key | Execute shell commands on remote Linux servers |
| `strava` | Strava | OAuth 2.0 | Fetch athletic activity logs |
| `telegram` | Telegram Bot | Bot Token | Send messages and process Telegram bot updates |
| `todoist` | Todoist | API Key | Create tasks and manage project sections |
| `toggl` | Toggl Track | API Key | Log time tracking entries |
| `totp` | TOTP Generator | None | Generate 6-digit 2FA authentication codes |
| `trello` | Trello | OAuth 2.0 | Move cards across Kanban boards |
| `twitter` | Twitter / X | OAuth 2.0 | Post tweets and monitor hashtag mentions |
| `typeform` | Typeform | OAuth / Personal | Listen for form submissions |
| `wait` | Delay / Sleep | None | Pause workflow execution for specified seconds |
| `weather` | Weather | API Key | Fetch weather forecasts for location coordinates |
| `webhook` | Webhook Trigger | Ingress URL | Universal JSON payload ingestion endpoint |
| `wikipedia` | Wikipedia | None | Search articles and extract summary text |
| `wordpress` | WordPress | Application Password | Create blog posts and manage media uploads |
| `xml` | XML Processor | None | Parse XML files to JSON data structures |
| `youtube` | YouTube | OAuth 2.0 | Upload videos and monitor channel comments |

---

## Dynamic Variable Injection Across Integrations

Outputs from any integration step can be mapped directly into downstream nodes.

### Universal Syntax
```json
{{steps.<step_id_or_index>.<output_property>}}
```

### Practical Variable Chaining Example
1. **Step 1 (Webhook Trigger):** Ingests incoming payload `{"customer_email": "jane@company.com", "issue_summary": "Login page error"}`
2. **Step 2 (OpenAI Action):** Analyzes sentiment using `{{steps.trigger.issue_summary}}`
3. **Step 3 (Jira Action):** Creates issue with description `{{steps.2.choices[0].message.content}}`
4. **Step 4 (Slack Action):** Posts channel alert: *"New issue created by {{steps.trigger.customer_email}} - Ticket ID: {{steps.3.key}}"*
