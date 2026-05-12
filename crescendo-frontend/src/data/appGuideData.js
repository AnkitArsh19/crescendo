/**
 * App Setup Guide Data — curated onboarding content for each app.
 *
 * Each entry provides:
 *   - description:  What the app does (1-2 sentences)
 *   - returns:      What data it outputs (for workflow chaining)
 *   - examples:     2-3 real workflow use-case ideas
 *   - authType:     'oauth' | 'apikey' | 'none' — drives the setup steps
 *   - setupSteps:   Numbered setup instructions (title, detail, optional link/codeSnippet)
 *   - callbackUrl:  For OAuth apps — the redirect URI to add in the developer portal
 *   - outputFields: Key output fields (shown in Reference tab)
 *
 * For apps not listed here, we auto-generate a basic guide from the catalog metadata.
 */

function getBaseUrl() {
  if (typeof window !== 'undefined' && window.location.hostname !== 'localhost') {
    return `https://api.crescendo.run`;
  }
  return `${window.location.protocol}//api.crescendo.run`;
}

export function getCallbackUrl(appKey) {
  return `${getBaseUrl()}/connections/oauth/${appKey}/callback`;
}

export const APP_GUIDES = {
  // ═══════════════════════════════════════════════════════════════════════════
  // AI & Machine Learning
  // ═══════════════════════════════════════════════════════════════════════════

  'gemini': {
    description: 'Generate text, answer questions, summarize content, and more using Google\'s Gemini AI models.',
    returns: 'Generated text response, model name, token usage stats',
    category: 'AI & Machine Learning',
    examples: [
      'Summarize incoming emails and post highlights to Slack',
      'Generate product descriptions from a Google Sheets list',
      'Auto-reply to Discord messages with AI-generated responses',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Go to Google AI Studio',
        detail: 'Open the Google AI Studio dashboard where you can create and manage API keys.',
        link: 'https://aistudio.google.com/app/apikey',
      },
      {
        title: 'Sign in with Google',
        detail: 'Use your Google account to sign in. The free tier gives you 60 requests per minute.',
      },
      {
        title: 'Create an API Key',
        detail: 'Click "Create API key" and select or create a Google Cloud project. The key will be generated instantly.',
      },
      {
        title: 'Copy the API Key',
        detail: 'Copy the key (starts with "AIza...") and paste it in the credential field below.',
      },
    ],
    outputFields: [
      { name: 'text', desc: 'The generated text response' },
      { name: 'model', desc: 'Model used (e.g. gemini-2.0-flash)' },
      { name: 'response', desc: 'Full API response object' },
    ],
  },

  'openai': {
    description: 'Generate text, images, and embeddings using OpenAI\'s GPT and DALL·E models.',
    returns: 'Generated text, model info, token usage',
    category: 'AI & Machine Learning',
    examples: [
      'Generate blog post drafts from RSS feed titles',
      'Classify customer support emails and route to the right team in Slack',
      'Create image descriptions for Google Drive files',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Open API Keys page',
        detail: 'Go to the OpenAI platform and navigate to the API keys section.',
        link: 'https://platform.openai.com/api-keys',
      },
      {
        title: 'Create a new API key',
        detail: 'Click "Create new secret key", give it a name like "Crescendo", and copy the key immediately — it won\'t be shown again.',
      },
      {
        title: 'Check billing',
        detail: 'OpenAI API requires a payment method. Add one at platform.openai.com/settings/organization/billing. Free credits may be available for new accounts.',
        link: 'https://platform.openai.com/settings/organization/billing',
      },
      {
        title: 'Paste the key',
        detail: 'Paste your API key (starts with "sk-...") in the credential field.',
      },
    ],
    outputFields: [
      { name: 'text', desc: 'Generated completion text' },
      { name: 'model', desc: 'Model used (e.g. gpt-4o)' },
      { name: 'usage', desc: 'Token usage breakdown' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Communication
  // ═══════════════════════════════════════════════════════════════════════════

  'discord': {
    description: 'Send messages, rich embeds, and manage channels in Discord. Supports both Bot token (for actions) and OAuth2 user login (for server/channel discovery).',
    returns: 'Message ID, channel info, server details, delivery status',
    category: 'Communication',
    examples: [
      'Post GitHub PR notifications to a #dev channel',
      'Send daily weather updates to your server',
      'Forward new RSS articles as rich embeds',
    ],
    authType: 'dual',
    setupSteps: [
      {
        title: 'Go to Discord Developer Portal',
        detail: 'Open the Discord developer dashboard. You can connect via Bot Token (for sending messages) or OAuth2 login (for discovering your servers/channels).',
        link: 'https://discord.com/developers/applications',
      },
      {
        title: 'Option A: Bot Token (recommended for actions)',
        detail: 'Create a New Application → Bot tab → Reset Token → Copy the token. This lets Crescendo send messages, manage channels, and assign roles on your behalf.',
      },
      {
        title: 'Option B: OAuth2 Login (for discovery)',
        detail: 'Click "Connect with Discord" to sign in with your Discord account. This lets Crescendo see all servers and channels you belong to — useful for picking where to send messages.',
      },
      {
        title: 'Enable Required Bot Intents',
        detail: 'In Developer Portal → Bot tab → scroll to "Privileged Gateway Intents" → enable SERVER MEMBERS INTENT and MESSAGE CONTENT INTENT.',
      },
      {
        title: 'Invite Bot to Your Server',
        detail: 'Go to "OAuth2 → URL Generator". Select scope: "bot". Select permissions: Send Messages, Read Message History, Manage Channels, Manage Roles, Embed Links, Add Reactions. Copy the URL and open it to invite.',
      },
    ],
    outputFields: [
      { name: 'response', desc: 'Discord API response' },
      { name: 'channelId', desc: 'Channel the message was sent to' },
      { name: 'content', desc: 'Message content' },
    ],
  },

  'slack': {
    description: 'Send messages, create channels, and interact with your Slack workspace.',
    returns: 'Message status, channel info, thread details',
    category: 'Communication',
    examples: [
      'Send daily standup reminders to #engineering',
      'Forward new GitHub issues to a Slack channel',
      'Post AI-generated summaries of long email threads',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Go to Slack API',
        detail: 'Open the Slack API dashboard to create your app.',
        link: 'https://api.slack.com/apps',
      },
      {
        title: 'Create New App',
        detail: 'Click "Create New App" → "From scratch". Name it (e.g. "Crescendo") and select your workspace.',
      },
      {
        title: 'Add Bot Scopes',
        detail: 'Go to "OAuth & Permissions" → "Scopes" → "Bot Token Scopes". Add: chat:write, channels:read, channels:history.',
      },
      {
        title: 'Install to Workspace',
        detail: 'Click "Install to Workspace" at the top of the OAuth page and authorize. Copy the "Bot User OAuth Token" (starts with xoxb-).',
      },
      {
        title: 'Invite Bot to Channel',
        detail: 'In Slack, go to the channel you want to use, type /invite @YourBotName to add it.',
      },
    ],
    outputFields: [
      { name: 'response', desc: 'Slack API response' },
      { name: 'channel', desc: 'Channel ID' },
      { name: 'text', desc: 'Message sent' },
    ],
  },

  'telegram': {
    description: 'Send messages, photos, and documents to Telegram chats and groups.',
    returns: 'Message ID, chat info, delivery status',
    category: 'Communication',
    examples: [
      'Send server monitoring alerts to a Telegram group',
      'Forward new RSS articles to a Telegram channel',
      'Send daily task reminders from Google Tasks',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Open BotFather in Telegram',
        detail: 'Search for @BotFather in Telegram or open the link below.',
        link: 'https://t.me/BotFather',
      },
      {
        title: 'Create a New Bot',
        detail: 'Send /newbot to BotFather. Choose a name and a username (must end with "bot"). BotFather will give you a token.',
      },
      {
        title: 'Copy the Bot Token',
        detail: 'Copy the token that looks like: 123456789:ABCdefGHIjklMNOpqrstUVWxyz. Paste it in the credential field.',
      },
      {
        title: 'Get your Chat ID',
        detail: 'Send a message to your bot, then visit https://api.telegram.org/bot<YOUR_TOKEN>/getUpdates to find your chat ID.',
      },
    ],
    outputFields: [
      { name: 'response', desc: 'Telegram API response' },
      { name: 'messageId', desc: 'Sent message ID' },
      { name: 'chatId', desc: 'Target chat ID' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Google Workspace
  // ═══════════════════════════════════════════════════════════════════════════

  'gmail': {
    description: 'Send emails, read inbox, and watch for new messages using Gmail.',
    returns: 'Email subject, sender, body preview, labels, message ID',
    category: 'Communication',
    examples: [
      'Forward important emails to a Slack channel',
      'Auto-respond to emails matching certain keywords with Gemini-generated replies',
      'Log new emails to a Google Sheet',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Gmail"',
        detail: 'No manual setup needed! Click the connect button below to sign in with your Google account.',
      },
      {
        title: 'Authorize Access',
        detail: 'Google will ask you to grant Crescendo permission to read and send emails on your behalf. Review and approve.',
      },
      {
        title: 'Done!',
        detail: 'Once authorized, your connection is active. You can now use Gmail as a trigger or action in your workflows.',
      },
    ],
    outputFields: [
      { name: 'subject', desc: 'Email subject line' },
      { name: 'fromEmail', desc: 'Sender email address' },
      { name: 'snippet', desc: 'Preview of the email body' },
      { name: 'id', desc: 'Gmail message ID' },
    ],
  },

  'google-sheets': {
    description: 'Read, write, and update Google Sheets — perfect for data workflows.',
    returns: 'Cell values, spreadsheet ID, sheet names, ranges',
    category: 'Productivity',
    examples: [
      'Append job search results to a spreadsheet',
      'Read form responses and send personalized emails',
      'Export Airtable records to Google Sheets nightly',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account. No manual API key setup required.',
      },
      {
        title: 'Grant Spreadsheet Access',
        detail: 'Authorize Crescendo to view and manage your spreadsheets.',
      },
      {
        title: 'Select a Spreadsheet',
        detail: 'When configuring a workflow step, you\'ll see a dropdown listing all your spreadsheets.',
      },
    ],
    outputFields: [
      { name: 'values', desc: 'Array of cell values' },
      { name: 'range', desc: 'Sheet range (e.g. A1:D10)' },
      { name: 'spreadsheetId', desc: 'Spreadsheet identifier' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Developer Tools
  // ═══════════════════════════════════════════════════════════════════════════

  'github': {
    description: 'Create issues, manage PRs, watch repos, and interact with GitHub.',
    returns: 'Issue/PR details, repo info, comments, commit data',
    category: 'Developer Tools',
    examples: [
      'Post new GitHub issues to a Discord channel',
      'Auto-label PRs based on file changes using AI',
      'Track star counts and send weekly reports',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Open GitHub Token Settings',
        detail: 'Go to GitHub Settings → Developer Settings → Personal Access Tokens → Fine-grained tokens.',
        link: 'https://github.com/settings/tokens?type=beta',
      },
      {
        title: 'Generate New Token',
        detail: 'Click "Generate new token". Name it "Crescendo", set expiration, and select repos you want to access.',
      },
      {
        title: 'Select Permissions',
        detail: 'Grant permissions: Issues (Read/Write), Pull Requests (Read/Write), Contents (Read). You can add more later.',
      },
      {
        title: 'Copy and Paste',
        detail: 'Copy the generated token (starts with "github_pat_...") and paste it below.',
      },
    ],
    outputFields: [
      { name: 'action', desc: 'Event action (opened, closed, etc.)' },
      { name: 'title', desc: 'Issue/PR title' },
      { name: 'url', desc: 'Link to the resource' },
      { name: 'sender', desc: 'Who triggered the event' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Fun & Lifestyle
  // ═══════════════════════════════════════════════════════════════════════════

  'spotify': {
    description: 'Search tracks, albums, artists, and playlists. Manage your Spotify library and control playback.',
    returns: 'Track names, artists, album art URLs, playlist details, playback state',
    category: 'Fun & Lifestyle',
    examples: [
      'Search for songs and share results in Discord/Slack',
      'Get playlist tracks and export to Google Sheets',
      'Build a "song of the day" workflow that posts to Telegram',
    ],
    authType: 'dual', // OAuth (admin) or API Key (user)
    setupSteps: [
      {
        title: 'Go to Spotify Developer Dashboard',
        detail: 'Open the Spotify developer portal to create your app.',
        link: 'https://developer.spotify.com/dashboard',
      },
      {
        title: 'Create a New App',
        detail: 'Click "Create app". Name it (e.g. "Crescendo"), add a description, and accept the terms.',
      },
      {
        title: 'Set the Redirect URI',
        detail: 'In your app settings, add this as the Redirect URI:',
        codeSnippet: null, // Will be computed dynamically with getCallbackUrl('spotify')
        dynamicCallback: 'spotify',
      },
      {
        title: 'Copy Client ID',
        detail: 'From the app\'s settings page, copy the "Client ID" (a 32-character hex string).',
      },
      {
        title: 'Copy Client Secret',
        detail: 'Click "View client secret" and copy it. You\'ll need both the Client ID and Secret.',
      },
      {
        title: 'Paste Credentials',
        detail: 'Enter both the Client ID and Client Secret in the fields below.',
      },
    ],
    outputFields: [
      { name: 'tracks', desc: 'Array of matching tracks' },
      { name: 'artists', desc: 'Array of matching artists' },
      { name: 'albums', desc: 'Array of matching albums' },
      { name: 'playlists', desc: 'Array of matching playlists' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Productivity
  // ═══════════════════════════════════════════════════════════════════════════

  'job-search': {
    description: 'Search jobs across 10+ platforms — LinkedIn, Greenhouse (Razorpay, Swiggy, CRED), Lever, Adzuna, and more. India-focused.',
    returns: 'Job listings with title, company, location, URL, source, and salary info',
    category: 'Productivity',
    examples: [
      'Search "Software Engineer" in Bangalore and export results to Google Sheets',
      'Set up a daily search and get new jobs posted to your Telegram',
      'Search across LinkedIn with custom filters (paste a LinkedIn search URL!)',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required!',
        detail: '6 free sources work out of the box: LinkedIn, Greenhouse (15 Indian companies), Lever, Remotive, Arbeitnow, and Himalayas.',
      },
      {
        title: 'Optional: Add API keys for more sources',
        detail: 'For Google Jobs results (aggregates Naukri, Indeed India, Glassdoor), add a SerpAPI key. Free tier: 100 searches/month.',
        link: 'https://serpapi.com',
      },
      {
        title: 'Pro tip: LinkedIn URL filter',
        detail: 'Go to linkedin.com/jobs/search, apply your filters (experience, job type, date posted, remote), copy the full URL, and paste it in the "LinkedIn Search URL" field. All filters are preserved!',
        link: 'https://www.linkedin.com/jobs/search/',
      },
    ],
    outputFields: [
      { name: 'jobs', desc: 'Array of job listings' },
      { name: 'totalFound', desc: 'Number of unique results' },
      { name: 'sources', desc: 'Per-source stats and errors' },
      { name: 'query', desc: 'Search query used' },
      { name: 'location', desc: 'Location filter applied' },
    ],
  },

  'notion': {
    description: 'Create pages, query databases, and manage content in your Notion workspace.',
    returns: 'Page ID, title, URL, database entries, content blocks',
    category: 'Productivity',
    examples: [
      'Save job search results as Notion database entries',
      'Create meeting notes from Google Calendar events',
      'Track GitHub issues in a Notion board',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Go to Notion Integrations',
        detail: 'Create an internal integration in your Notion workspace.',
        link: 'https://www.notion.so/my-integrations',
      },
      {
        title: 'Create New Integration',
        detail: 'Click "New integration", name it "Crescendo", and select your workspace. Choose "Internal" type.',
      },
      {
        title: 'Copy the Token',
        detail: 'Copy the "Internal Integration Secret" (starts with "ntn_...") and paste it below.',
      },
      {
        title: 'Share Pages/Databases',
        detail: 'In Notion, open the page or database you want to use → click "..." → "Connections" → add your integration. The bot can only access pages you explicitly share.',
      },
    ],
    outputFields: [
      { name: 'id', desc: 'Page or database ID' },
      { name: 'url', desc: 'Notion page URL' },
      { name: 'title', desc: 'Page title' },
    ],
  },

  'airtable': {
    description: 'Read, create, and update records in your Airtable bases.',
    returns: 'Record ID, field values, table metadata',
    category: 'Productivity',
    examples: [
      'Add new leads from a webhook to an Airtable CRM',
      'Sync Google Form responses to Airtable',
      'Export Airtable records to Google Sheets',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Go to Airtable Token Page',
        detail: 'Open your Airtable account settings to create a personal access token.',
        link: 'https://airtable.com/create/tokens',
      },
      {
        title: 'Create Token',
        detail: 'Click "Create new token". Name it, add scopes: data.records:read, data.records:write. Select the bases you want to access.',
      },
      {
        title: 'Copy and Paste',
        detail: 'Copy the token (starts with "pat...") and paste it below.',
      },
    ],
    outputFields: [
      { name: 'id', desc: 'Record ID' },
      { name: 'fields', desc: 'Record field values' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Utilities
  // ═══════════════════════════════════════════════════════════════════════════

  'http': {
    description: 'Make HTTP requests to any API — GET, POST, PUT, DELETE with custom headers and body.',
    returns: 'Response body, status code, headers',
    category: 'Developer Tools',
    examples: [
      'Call any REST API and pass results to the next step',
      'Send webhooks to external services',
      'Integrate with APIs that aren\'t in the app catalog',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'The HTTP app lets you call any URL directly. No credentials needed for public APIs — just configure the URL, method, and body in the step settings.',
      },
    ],
    outputFields: [
      { name: 'body', desc: 'Response body (parsed JSON or text)' },
      { name: 'statusCode', desc: 'HTTP status code (200, 404, etc.)' },
      { name: 'headers', desc: 'Response headers' },
    ],
  },




  'rss': {
    description: 'Watch RSS/Atom feeds for new articles, posts, or updates.',
    returns: 'Article title, link, description, published date, author',
    category: 'Productivity',
    examples: [
      'Post new blog articles to a Discord channel',
      'Monitor competitor press releases',
      'Send Hacker News top stories to Telegram daily',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'Just provide the RSS feed URL in the step configuration. Works with any public RSS or Atom feed.',
      },
    ],
    outputFields: [
      { name: 'title', desc: 'Article title' },
      { name: 'link', desc: 'URL to the article' },
      { name: 'description', desc: 'Article summary' },
      { name: 'pubDate', desc: 'Published date' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Batch 6-9 Apps
  // ═══════════════════════════════════════════════════════════════════════════

  'sarvam': {
    description: 'Indian language AI — translate text, generate speech, and chat using Sarvam AI models.',
    returns: 'Translated text, audio data, chat completions',
    category: 'AI & Machine Learning',
    examples: [
      'Translate Slack messages from English to Hindi automatically',
      'Generate voice announcements from Google Sheets data',
      'Auto-translate customer support emails into regional languages',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Go to Sarvam AI',
        detail: 'Sign up for a free account at sarvam.ai. New accounts get free starter credits.',
        link: 'https://www.sarvam.ai/',
      },
      {
        title: 'Get your API Key',
        detail: 'Navigate to the dashboard and copy your API subscription key.',
      },
      {
        title: 'Paste the key',
        detail: 'Enter your API key in the credential field below.',
      },
    ],
    outputFields: [
      { name: 'translatedText', desc: 'Translated text output' },
      { name: 'audios', desc: 'Base64 encoded audio data' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'strava': {
    description: 'Track activities, create workouts, and manage your Strava athlete profile.',
    returns: 'Activity details, athlete stats, distance, time, and elevation data',
    category: 'Fun & Lifestyle',
    examples: [
      'Log completed Pomodoro sessions as Strava activities',
      'Post new activity summaries to a Slack channel',
      'Export weekly training stats to Google Sheets',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Strava"',
        detail: 'Click the connect button to authorize Crescendo with your Strava account.',
      },
      {
        title: 'Grant Access',
        detail: 'Strava will ask permission to read and create activities. Approve the request.',
      },
      {
        title: 'Done!',
        detail: 'Your Strava account is now connected. You can create and track activities.',
      },
    ],
    outputFields: [
      { name: 'activityId', desc: 'Strava activity ID' },
      { name: 'name', desc: 'Activity name' },
      { name: 'type', desc: 'Activity type (Run, Ride, etc.)' },
      { name: 'distance', desc: 'Distance in meters' },
    ],
  },

  'figma': {
    description: 'Watch for file updates, export assets, and track design changes in Figma.',
    returns: 'File metadata, version info, comments, exported images',
    category: 'Productivity',
    examples: [
      'Notify Slack when a Figma file is updated',
      'Export design assets and upload to Google Drive',
      'Track new comments on design files',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Figma"',
        detail: 'Click the connect button to authorize with your Figma account.',
      },
      {
        title: 'Authorize Access',
        detail: 'Figma will ask for permission to read your files, comments, and metadata.',
      },
    ],
    outputFields: [
      { name: 'fileKey', desc: 'Figma file key' },
      { name: 'name', desc: 'File name' },
      { name: 'lastModified', desc: 'Last modification date' },
    ],
  },

  'linear': {
    description: 'Create issues, track projects, and manage your Linear workspace.',
    returns: 'Issue details, state, priority, assignee, labels',
    category: 'Productivity',
    examples: [
      'Create Linear issues from Slack messages',
      'Notify Discord when issues are completed',
      'Sync GitHub PRs with Linear issues',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Linear"',
        detail: 'Click the connect button to authorize with your Linear workspace.',
      },
      {
        title: 'Authorize Access',
        detail: 'Linear will ask for permission to read and write issues and comments.',
      },
    ],
    outputFields: [
      { name: 'issueId', desc: 'Linear issue ID' },
      { name: 'title', desc: 'Issue title' },
      { name: 'state', desc: 'Issue state (Todo, In Progress, Done)' },
    ],
  },

  'leetcode': {
    description: 'Fetch LeetCode stats, daily challenges, and search problems by difficulty.',
    returns: 'User stats, problem details, difficulty, acceptance rate',
    category: 'Developer Tools',
    examples: [
      'Post the daily LeetCode challenge to Discord every morning',
      'Track your solving progress in Google Sheets',
      'Search problems by topic and share with study groups',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'LeetCode uses a public API. Just configure the username or search query in the step settings.',
      },
    ],
    outputFields: [
      { name: 'username', desc: 'LeetCode username' },
      { name: 'solved', desc: 'Problems solved count' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'weather': {
    description: 'Get current weather and 5-day forecasts for any city worldwide.',
    returns: 'Temperature, conditions, humidity, wind, forecast data',
    category: 'Utilities',
    examples: [
      'Send daily weather updates to a Telegram group',
      'Trigger notifications when temperature drops below threshold',
      'Log weather data to Google Sheets for analysis',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'Weather works with a demo API key. Just configure the city name in the step settings.',
      },
    ],
    outputFields: [
      { name: 'city', desc: 'City name' },
      { name: 'temperature', desc: 'Current temperature' },
      { name: 'description', desc: 'Weather conditions' },
    ],
  },

  'nasa-apod': {
    description: 'Get NASA\'s Astronomy Picture of the Day and Mars Rover photos.',
    returns: 'Image URL, title, explanation, rover data',
    category: 'Fun & Lifestyle',
    examples: [
      'Post the daily NASA picture to a Discord channel',
      'Collect Mars Rover photos in Google Drive',
      'Send space facts to Telegram groups',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'NASA uses a free DEMO_KEY. Just configure the date or rover settings.',
      },
    ],
    outputFields: [
      { name: 'title', desc: 'Image title' },
      { name: 'url', desc: 'Image URL' },
      { name: 'explanation', desc: 'Description text' },
    ],
  },

  'pomodoro': {
    description: 'Create focus timers, log work time, and calculate session durations.',
    returns: 'Timer start/end times, duration, session label',
    category: 'Productivity',
    examples: [
      'Start a focus timer and notify Slack when it ends',
      'Log completed work sessions to Google Sheets',
      'Chain timers with break reminders via Telegram',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'Pomodoro is a built-in utility. Configure duration and label in the step settings.',
      },
    ],
    outputFields: [
      { name: 'startTime', desc: 'Session start time (ISO)' },
      { name: 'endTime', desc: 'Session end time (ISO)' },
      { name: 'durationMinutes', desc: 'Duration in minutes' },
    ],
  },

  'crescendo-email': {
    description: 'Send transactional emails using Crescendo\'s built-in email service — no credentials needed.',
    returns: 'Delivery status, message ID',
    category: 'Communication',
    examples: [
      'Send email notifications when a workflow completes',
      'Forward important alerts to your team',
      'Send daily report summaries',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'No setup required',
        detail: 'Crescendo Email is built-in. Just configure the recipient, subject, and body.',
      },
    ],
    outputFields: [
      { name: 'to', desc: 'Recipient email' },
      { name: 'subject', desc: 'Email subject' },
      { name: 'response', desc: 'Delivery response' },
    ],
  },

  'crescendo-webhook': {
    description: 'Receive incoming HTTP webhooks from any external service, or send POST requests to external endpoints.',
    returns: 'Request body, headers, HTTP method, response data',
    category: 'Developer Tools',
    examples: [
      'Trigger a workflow when a payment gateway sends an event',
      'Process form submissions from your website',
      'Send data to external services via POST',
    ],
    authType: 'none',
    setupSteps: [
      {
        title: 'Create a Workflow with Webhook Trigger',
        detail: 'Add a webhook trigger to your workflow. You\'ll get a unique URL that accepts POST requests.',
      },
      {
        title: 'Send data to the URL',
        detail: 'Configure your external service to send POST requests to the webhook URL. The body and headers are available as step data.',
      },
    ],
    outputFields: [
      { name: 'body', desc: 'Parsed request body' },
      { name: 'headers', desc: 'Request headers' },
      { name: 'method', desc: 'HTTP method used' },
    ],
  },
};


/**
 * Returns the guide for an app, or generates a basic fallback from catalog metadata.
 */
export function getAppGuide(appKey, catalogApp) {
  if (APP_GUIDES[appKey]) return APP_GUIDES[appKey];

  // Auto-generate basic guide from catalog metadata
  if (!catalogApp) return null;

  const isOAuth = catalogApp.authType === 'OAUTH2';
  const isNone = catalogApp.authType === 'NONE';

  return {
    description: catalogApp.description || `Connect and interact with ${catalogApp.name}.`,
    returns: 'Data from the app\'s API responses',
    category: catalogApp.category || 'Other',
    examples: [],
    authType: isNone ? 'none' : isOAuth ? 'oauth' : 'apikey',
    setupSteps: isNone
      ? [{ title: 'No setup required', detail: `${catalogApp.name} works without any credentials.` }]
      : isOAuth
      ? [
          { title: `Connect with ${catalogApp.name}`, detail: 'Click the connect button to sign in via OAuth. No manual setup needed.' },
        ]
      : [
          { title: 'Get your API credentials', detail: `Visit the ${catalogApp.name} developer portal to create an API key.`, link: catalogApp.helpUrl || null },
          { title: 'Paste credentials below', detail: 'Enter your API key or token in the credential fields.' },
        ],
    outputFields: [],
  };
}
