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

  // ═══════════════════════════════════════════════════════════════════════════
  // Google Workspace — Additional Apps
  // ═══════════════════════════════════════════════════════════════════════════

  'google-drive': {
    description: 'Upload, download, and manage files and folders in Google Drive.',
    returns: 'File ID, name, MIME type, download links, folder structure',
    category: 'Productivity',
    examples: [
      'Upload CSV exports from workflows to Google Drive',
      'Organize attachments from Gmail into labeled Drive folders',
      'List files in a shared folder and send links via Slack',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account. No manual API key setup required.',
      },
      {
        title: 'Grant Drive Access',
        detail: 'Authorize Crescendo to view and manage your Google Drive files and folders.',
      },
      {
        title: 'Done!',
        detail: 'Once authorized, you can upload, list, copy, move, and delete files in your Drive.',
      },
    ],
    outputFields: [
      { name: 'fileId', desc: 'Google Drive file ID' },
      { name: 'name', desc: 'File or folder name' },
      { name: 'mimeType', desc: 'MIME type (e.g. application/pdf)' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'google-calendar': {
    description: 'Create, update, delete, and list events in Google Calendar.',
    returns: 'Event ID, title, start/end times, attendees, location',
    category: 'Productivity',
    examples: [
      'Create calendar events from new Airtable records',
      'Send Slack reminders 30 minutes before upcoming meetings',
      'Log completed calendar events to Google Sheets',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account.',
      },
      {
        title: 'Grant Calendar Access',
        detail: 'Authorize Crescendo to view and manage your Google Calendar events.',
      },
      {
        title: 'Select a Calendar',
        detail: 'When configuring a step, choose which calendar to use (primary or any shared calendar).',
      },
    ],
    outputFields: [
      { name: 'eventId', desc: 'Calendar event ID' },
      { name: 'summary', desc: 'Event title' },
      { name: 'start', desc: 'Event start time' },
      { name: 'end', desc: 'Event end time' },
      { name: 'attendees', desc: 'List of attendees' },
    ],
  },

  'google-docs': {
    description: 'Create, read, and edit Google Docs — append text, replace content, and retrieve document data.',
    returns: 'Document ID, title, body content, revision info',
    category: 'Productivity',
    examples: [
      'Generate meeting notes from Google Calendar events',
      'Append daily summaries to a shared team document',
      'Search-and-replace placeholder text in document templates',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account.',
      },
      {
        title: 'Grant Docs Access',
        detail: 'Authorize Crescendo to view and edit your Google Docs.',
      },
      {
        title: 'Done!',
        detail: 'You can now create documents, append text, and replace content programmatically.',
      },
    ],
    outputFields: [
      { name: 'documentId', desc: 'Google Doc ID' },
      { name: 'title', desc: 'Document title' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'google-tasks': {
    description: 'Create, update, complete, and list tasks in Google Tasks.',
    returns: 'Task ID, title, status, due date, notes',
    category: 'Productivity',
    examples: [
      'Create tasks from new GitHub issues',
      'Mark tasks complete when Slack messages are reacted to',
      'List overdue tasks and send daily reminders via Telegram',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account.',
      },
      {
        title: 'Grant Tasks Access',
        detail: 'Authorize Crescendo to view and manage your Google Tasks.',
      },
      {
        title: 'Select a Task List',
        detail: 'When configuring a step, choose which task list to use.',
      },
    ],
    outputFields: [
      { name: 'taskId', desc: 'Google Task ID' },
      { name: 'title', desc: 'Task title' },
      { name: 'status', desc: 'Task status (needsAction, completed)' },
      { name: 'due', desc: 'Due date (RFC 3339)' },
    ],
  },

  'google-forms': {
    description: 'Create forms, retrieve form metadata, and list responses from Google Forms.',
    returns: 'Form ID, title, response data, submission timestamps',
    category: 'Productivity',
    examples: [
      'Collect form responses and send them to a Slack channel',
      'Export form submissions to Google Sheets automatically',
      'Create feedback forms from Notion database entries',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account.',
      },
      {
        title: 'Grant Forms Access',
        detail: 'Authorize Crescendo to view your forms and read responses.',
      },
      {
        title: 'Done!',
        detail: 'You can now create forms, read form structure, and list all submitted responses.',
      },
    ],
    outputFields: [
      { name: 'formId', desc: 'Google Form ID' },
      { name: 'title', desc: 'Form title' },
      { name: 'responses', desc: 'Array of form responses' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'google-slides': {
    description: 'Create presentations, add slides, and retrieve presentation data from Google Slides.',
    returns: 'Presentation ID, title, slide IDs, layout info',
    category: 'Productivity',
    examples: [
      'Generate weekly report presentations from Google Sheets data',
      'Add a new slide for each completed sprint from Linear',
      'Retrieve presentation metadata and share links via email',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Google"',
        detail: 'Click the OAuth connect button to sign in with your Google account.',
      },
      {
        title: 'Grant Slides Access',
        detail: 'Authorize Crescendo to view and edit your Google Slides presentations.',
      },
      {
        title: 'Done!',
        detail: 'You can now create presentations, add slides, and read presentation content.',
      },
    ],
    outputFields: [
      { name: 'presentationId', desc: 'Google Slides presentation ID' },
      { name: 'title', desc: 'Presentation title' },
      { name: 'slides', desc: 'Array of slide objects' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Microsoft Graph — Outlook, Excel, Teams
  // ═══════════════════════════════════════════════════════════════════════════

  'microsoft-outlook': {
    description: 'Send emails, create drafts, manage calendar events, and organize mail folders via Microsoft Outlook.',
    returns: 'Message ID, event ID, subject, recipients, status',
    category: 'Communication',
    examples: [
      'Send follow-up emails when a task is completed in Asana',
      'Create Outlook calendar events from Google Sheets rows',
      'Move incoming support emails to a specific folder',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Microsoft"',
        detail: 'Click the OAuth connect button to sign in with your Microsoft account. Works with both personal and work/school accounts.',
      },
      {
        title: 'Grant Mail & Calendar Access',
        detail: 'Authorize Crescendo to read/write emails, send mail, and manage calendar events.',
      },
      {
        title: 'Done!',
        detail: 'You can now send emails, create drafts, reply to messages, move emails, and create calendar events.',
      },
    ],
    outputFields: [
      { name: 'messageId', desc: 'Outlook message ID' },
      { name: 'eventId', desc: 'Calendar event ID' },
      { name: 'subject', desc: 'Email or event subject' },
      { name: 'statusCode', desc: 'HTTP response status' },
    ],
  },

  'microsoft-excel': {
    description: 'Read, write, and manage Excel workbooks stored in OneDrive via Microsoft Graph.',
    returns: 'Row data, workbook/worksheet IDs, cell values',
    category: 'Productivity',
    examples: [
      'Append new leads from a form submission to an Excel spreadsheet',
      'Search for a customer by email in an Excel workbook',
      'Create a new Excel workbook for each monthly report',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Microsoft"',
        detail: 'Click the OAuth connect button to sign in with your Microsoft account.',
      },
      {
        title: 'Grant OneDrive Access',
        detail: 'Authorize Crescendo to access and modify your Excel files stored in OneDrive.',
      },
      {
        title: 'Select a Workbook',
        detail: 'When configuring a step, pick the workbook and worksheet from the dropdown.',
      },
    ],
    outputFields: [
      { name: 'driveItemId', desc: 'OneDrive item ID for the workbook' },
      { name: 'worksheetId', desc: 'Worksheet ID within the workbook' },
      { name: 'values', desc: 'Row data as arrays' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  'microsoft-teams': {
    description: 'Send channel messages, direct chat messages, and create channels in Microsoft Teams.',
    returns: 'Message ID, chat ID, channel ID, team info',
    category: 'Communication',
    examples: [
      'Post deployment notifications to a Teams channel',
      'Send a direct message to a manager when a form is submitted',
      'Create a new Teams channel for each new client project',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Click "Connect with Microsoft"',
        detail: 'Click the OAuth connect button to sign in with your Microsoft work/school account.',
      },
      {
        title: 'Grant Teams Access',
        detail: 'Authorize Crescendo to read your teams/channels and send messages on your behalf.',
      },
      {
        title: 'Select a Team & Channel',
        detail: 'When configuring a step, pick the team and channel from the dropdowns.',
      },
    ],
    outputFields: [
      { name: 'teamId', desc: 'Microsoft Teams team ID' },
      { name: 'channelId', desc: 'Channel ID within the team' },
      { name: 'chatId', desc: 'Chat ID for direct messages' },
      { name: 'response', desc: 'Full API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Phase 3 — Slack, Discord, Telegram
  // ═══════════════════════════════════════════════════════════════════════════

  'slack': {
    description: 'Send messages, search conversations, manage channels, and automate Slack workflows.',
    returns: 'Channel ID, message timestamp, search results, response status',
    category: 'Communication',
    examples: [
      'Post deployment notifications to a Slack channel',
      'Send a direct message to a team lead when a form is submitted',
      'Search Slack messages for a keyword and log results',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Connect via OAuth or Bot Token',
        detail: 'Click "Connect with Slack" to authorize via OAuth, or paste a Bot User OAuth Token (xoxb-...) from your Slack app settings.',
      },
      {
        title: 'Invite the Bot to Channels',
        detail: 'Ensure your Slack bot has been invited to the channels you want to post in. Use /invite @botname in the channel.',
      },
      {
        title: 'Done!',
        detail: 'You can now send messages, create channels, search, add reactions, and invite users.',
      },
    ],
    outputFields: [
      { name: 'channel', desc: 'Slack channel ID' },
      { name: 'ts', desc: 'Message timestamp (unique message ID)' },
      { name: 'response', desc: 'Full Slack API response' },
    ],
  },

  'discord': {
    description: 'Send messages, manage roles, create channels, and automate Discord server actions.',
    returns: 'Message ID, channel ID, guild info, role assignment status',
    category: 'Communication',
    examples: [
      'Send a welcome message when a new member joins a Discord server',
      'Post rich embed notifications to a project channel',
      'Assign a role to a user when they complete onboarding',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Create a Discord Bot',
        detail: 'Go to the Discord Developer Portal, create an application, and add a Bot. Copy the Bot Token.',
      },
      {
        title: 'Invite the Bot to Your Server',
        detail: 'Generate an invite URL with the required permissions (Send Messages, Manage Channels, Manage Roles) and add the bot to your server.',
      },
      {
        title: 'Paste the Bot Token',
        detail: 'Enter your Bot Token in the connection settings. The bot will use this to authenticate with the Discord API.',
      },
    ],
    outputFields: [
      { name: 'channelId', desc: 'Discord channel ID' },
      { name: 'guildId', desc: 'Discord server (guild) ID' },
      { name: 'messageId', desc: 'Sent message ID' },
      { name: 'response', desc: 'Full Discord API response' },
    ],
  },

  'telegram': {
    description: 'Send messages, photos, documents, and locations via a Telegram Bot.',
    returns: 'Message ID, chat ID, file ID, response status',
    category: 'Communication',
    examples: [
      'Send order confirmation messages to customers via Telegram',
      'Share a PDF report to a Telegram group',
      'Send a location pin when a delivery is dispatched',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Create a Telegram Bot',
        detail: 'Message @BotFather on Telegram and use the /newbot command. Copy the API token provided.',
      },
      {
        title: 'Paste the Bot Token',
        detail: 'Enter the bot token as the API Key in the connection settings.',
      },
      {
        title: 'Get Chat IDs',
        detail: 'Send a message to your bot, then use the getUpdates API to find the chat_id for each conversation.',
      },
    ],
    outputFields: [
      { name: 'chatId', desc: 'Telegram chat ID' },
      { name: 'messageId', desc: 'Sent message ID' },
      { name: 'response', desc: 'Full Telegram Bot API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Phase 4 — Developer Tools + LinkedIn
  // ═══════════════════════════════════════════════════════════════════════════

  'gitlab': {
    description: 'Create issues, merge requests, and add comments in GitLab projects.',
    returns: 'Issue ID, MR IID, project info, API response',
    category: 'Developer Tools',
    examples: [
      'Create a bug report issue when a form is submitted',
      'Open a merge request from a workflow trigger',
      'Add a comment to an issue when a task is completed',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Connect via OAuth',
        detail: 'Click "Connect with GitLab" to authorize. Your GitLab access token will be stored securely.',
      },
      {
        title: 'Select a Project',
        detail: 'In the workflow step config, select the GitLab project and provide required details.',
      },
    ],
    outputFields: [
      { name: 'issueId', desc: 'GitLab issue ID' },
      { name: 'mrIid', desc: 'Merge request IID' },
      { name: 'response', desc: 'Full GitLab API response' },
    ],
  },

  'toggl': {
    description: 'Track time entries, start/stop timers, and manage workspaces in Toggl Track.',
    returns: 'Time entry ID, duration, project info',
    category: 'Productivity',
    examples: [
      'Start a timer when a task is assigned in Linear',
      'Create a completed time entry for a meeting',
      'Stop the current running timer when a workflow completes',
    ],
    authType: 'apikey',
    setupSteps: [
      {
        title: 'Get Your Toggl API Token',
        detail: 'Go to Toggl Track → Profile Settings → API Token. Copy the token.',
      },
      {
        title: 'Paste the API Token',
        detail: 'Enter the token in the connection settings. Toggl uses Basic auth with your token.',
      },
    ],
    outputFields: [
      { name: 'entryId', desc: 'Time entry ID' },
      { name: 'description', desc: 'Entry description' },
      { name: 'duration', desc: 'Duration in seconds' },
    ],
  },

  'linkedin': {
    description: 'Share posts and links on LinkedIn, and retrieve profile information.',
    returns: 'Post ID, profile data, API response',
    category: 'Social Media',
    examples: [
      'Auto-share a blog post link on LinkedIn when published',
      'Post a team update when a milestone is reached',
      'Retrieve LinkedIn profile info for onboarding workflows',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Connect via OAuth',
        detail: 'Click "Connect with LinkedIn" to authorize. Ensure your LinkedIn app has Community Management API access for posting.',
      },
      {
        title: 'Compose Your Post',
        detail: 'Enter the text content and optional link URL in the workflow step configuration.',
      },
    ],
    outputFields: [
      { name: 'postId', desc: 'LinkedIn post ID' },
      { name: 'response', desc: 'Full LinkedIn API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Phase 5 — Twitter/X
  // ═══════════════════════════════════════════════════════════════════════════

  'twitter': {
    description: 'Post and delete tweets on X (formerly Twitter) via the v2 API.',
    returns: 'Tweet ID, API response',
    category: 'Social Media',
    examples: [
      'Auto-tweet a summary when a blog post is published',
      'Delete a tweet as part of a moderation workflow',
    ],
    authType: 'oauth',
    setupSteps: [
      {
        title: 'Connect via OAuth',
        detail: 'Click "Connect with X" to authorize via OAuth 2.0 PKCE. Ensure your X Developer App has "Read and Write" permissions.',
      },
      {
        title: 'Compose Your Tweet',
        detail: 'Enter the tweet text in the workflow step configuration. X enforces a 280-character limit.',
      },
    ],
    outputFields: [
      { name: 'tweetId', desc: 'Posted tweet ID' },
      { name: 'response', desc: 'Full X API response' },
    ],
  },

  // ═══════════════════════════════════════════════════════════════════════════
  // Phase 6 & 7 — Public APIs + Internal/Utility
  // ═══════════════════════════════════════════════════════════════════════════

  'nasa': {
    description: 'Fetch NASA Astronomy Picture of the Day and Mars rover photos.',
    returns: 'Image URL, title, explanation, photo data',
    category: 'Science',
    examples: ['Get today\'s astronomy picture', 'Fetch Mars rover photos from Curiosity'],
    authType: 'apikey',
    setupSteps: [
      { title: 'Get a NASA API Key', detail: 'Register at api.nasa.gov for a free key, or use DEMO_KEY for testing.' },
      { title: 'Paste the Key', detail: 'Enter the API key in the connection settings.' },
    ],
    outputFields: [
      { name: 'title', desc: 'Image title' },
      { name: 'url', desc: 'Image or video URL' },
      { name: 'explanation', desc: 'Description of the image' },
    ],
  },

  'giphy': {
    description: 'Search, get random, or browse trending GIFs from Giphy.',
    returns: 'GIF URL, title, embed URL',
    category: 'Fun',
    examples: ['Search for cat GIFs', 'Get a random GIF', 'Browse trending GIFs'],
    authType: 'apikey',
    setupSteps: [
      { title: 'Get a Giphy API Key', detail: 'Create an app at developers.giphy.com to get an API key.' },
      { title: 'Paste the Key', detail: 'Enter the API key in the connection settings.' },
    ],
    outputFields: [{ name: 'response', desc: 'Giphy API response with GIF data' }],
  },

  'jokeapi': {
    description: 'Get random jokes filtered by category and type.',
    returns: 'Joke text, category, type',
    category: 'Fun',
    examples: ['Get a random programming joke', 'Fetch a safe-mode joke for a team chat'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'JokeAPI is a free public API. No API key required.' },
    ],
    outputFields: [{ name: 'response', desc: 'Joke data including setup and delivery' }],
  },

  'catfacts': {
    description: 'Get fun cat facts — one at a time or in bulk.',
    returns: 'Cat fact text',
    category: 'Fun',
    examples: ['Get a random cat fact', 'Get 5 cat facts for a newsletter'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Cat Facts is a free public API. No API key required.' },
    ],
    outputFields: [{ name: 'fact', desc: 'Cat fact text' }],
  },

  'quotes': {
    description: 'Get inspirational quotes — random, by category, or quote of the day.',
    returns: 'Quote text, author',
    category: 'Productivity',
    examples: ['Get a motivational quote for daily standup', 'Fetch quote of the day for a Slack message'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Quotes uses ZenQuotes, a free public API. No key required.' },
    ],
    outputFields: [{ name: 'response', desc: 'Quote data including text and author' }],
  },

  'githubstats': {
    description: 'Get public GitHub profile stats and repository listings without authentication.',
    returns: 'Profile info, repo list, follower count',
    category: 'Developer Tools',
    examples: ['Get a developer\'s public GitHub profile', 'List public repos for a user'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Uses GitHub\'s public API. No authentication required for public data.' },
    ],
    outputFields: [
      { name: 'login', desc: 'GitHub username' },
      { name: 'public_repos', desc: 'Number of public repos' },
      { name: 'followers', desc: 'Follower count' },
    ],
  },

  'log': {
    description: 'Print a message to the workflow execution log for debugging.',
    returns: 'Log message confirmation',
    category: 'Utility',
    examples: ['Log variable values for debugging', 'Print a status message between steps'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Log is a built-in utility. Just add it as a step.' },
    ],
    outputFields: [{ name: 'message', desc: 'The logged message' }],
  },

  'email': {
    description: 'Send emails via Crescendo\'s built-in email service.',
    returns: 'Delivery status, message ID',
    category: 'Communication',
    examples: ['Send a notification email when a workflow completes', 'Email a report to a stakeholder'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Uses Crescendo\'s built-in email service. Just configure recipient and message.' },
    ],
    outputFields: [{ name: 'response', desc: 'Email delivery status' }],
  },

  'webhook': {
    description: 'Send HTTP POST requests to external webhook URLs or receive incoming webhooks as triggers.',
    returns: 'Webhook response, status code',
    category: 'Utility',
    examples: ['Send data to a Zapier webhook', 'Trigger a workflow from an external system'],
    authType: 'none',
    setupSteps: [
      { title: 'Configure the URL', detail: 'For outgoing webhooks, enter the target URL. For incoming, use the generated Crescendo webhook URL.' },
    ],
    outputFields: [{ name: 'response', desc: 'Webhook response body' }],
  },

  'jobsearch': {
    description: 'Search for job listings and remote positions from aggregated job boards.',
    returns: 'Job title, company, URL, location',
    category: 'Productivity',
    examples: ['Search for remote React developer jobs', 'Find job listings matching a keyword'],
    authType: 'none',
    setupSteps: [
      { title: 'No Setup Needed', detail: 'Job Search uses public job APIs. No key required.' },
    ],
    outputFields: [{ name: 'response', desc: 'Job listing data' }],
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
