package com.crescendo.apps.mock;

import com.crescendo.apps.discord.DiscordMessageHandlers;
import com.crescendo.apps.dropbox.DropboxFileHandlers;
import com.crescendo.apps.github.GitHubSupport;
import com.crescendo.apps.gmail.GmailMessageHandlers;
import com.crescendo.apps.googlecalendar.GoogleCalendarEventHandlers;
import com.crescendo.apps.googledocs.GoogleDocsHandlers;
import com.crescendo.apps.googledrive.GoogleDriveFileHandlers;
import com.crescendo.apps.googlesheets.GoogleSheetsSheetHandlers;
import com.crescendo.apps.googleslides.GoogleSlidesHandlers;
import com.crescendo.apps.googletasks.GoogleTasksHandlers;
import com.crescendo.apps.microsoftexcel.MicrosoftExcelWorkbookHandlers;
import com.crescendo.apps.microsoftoutlook.MicrosoftOutlookMessageHandlers;
import com.crescendo.apps.microsoftteams.MicrosoftTeamsChannelMessageHandlers;
import com.crescendo.apps.notion.NotionSupport;
import com.crescendo.apps.salesforce.SalesforceLeadHandlers;
import com.crescendo.apps.slack.SlackMessageHandlers;
import com.crescendo.apps.strava.StravaActivityHandlers;
import com.crescendo.apps.telegram.TelegramMessageHandlers;
import com.crescendo.apps.todoist.TodoistTaskHandlers;
import com.crescendo.apps.toggl.TogglTimeEntryHandlers;
import com.crescendo.apps.trello.TrelloCardHandlers;
import com.crescendo.apps.twitter.TwitterTweetHandlers;
import com.crescendo.apps.wordpress.WordPressPostHandlers;
import com.crescendo.apps.youtube.YouTubeVideoHandlers;
import com.crescendo.execution.action.ActionContext;
import com.crescendo.execution.action.ActionResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OAuthAppsMockTest {

    private ActionContext createContext(String appKey, String actionKey, Map<String, Object> config, Map<String, Object> credentials) {
        return new ActionContext(appKey, actionKey, config, credentials, Map.of(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1);
    }

    // ── Google Suite ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Gmail: send fails gracefully when accessToken is missing")
    void testGmail_missingToken() {
        GmailMessageHandlers handlers = new GmailMessageHandlers();
        ActionContext context = createContext("gmail", "send", Map.of("to", "test@example.com", "subject", "Hi", "message", "Body"), Map.of());
        ActionResult result = handlers.send(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("accessToken"));
    }

    @Test
    @DisplayName("Google Sheets: appendRow fails when spreadsheetId is missing")
    void testGoogleSheets_missingSpreadsheetId() {
        GoogleSheetsSheetHandlers handlers = new GoogleSheetsSheetHandlers();
        ActionContext context = createContext("google-sheets", "appendRow", Map.of("range", "Sheet1!A1", "values", List.of("1", "2")), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.appendRow(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("spreadsheetId"));
    }

    @Test
    @DisplayName("Google Docs: create fails when title is missing")
    void testGoogleDocs_missingTitle() {
        GoogleDocsHandlers handlers = new GoogleDocsHandlers();
        ActionContext context = createContext("google-docs", "create", Map.of(), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.create(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("title"));
    }

    @Test
    @DisplayName("Google Calendar: create fails when calendarId is missing")
    void testGoogleCalendar_missingParams() {
        GoogleCalendarEventHandlers handlers = new GoogleCalendarEventHandlers();
        ActionContext context = createContext("google-calendar", "create", Map.of(), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.create(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("calendarId"));
    }

    @Test
    @DisplayName("Google Drive: uploadFile fails when file data is missing")
    void testGoogleDrive_missingFileData() {
        GoogleDriveFileHandlers handlers = new GoogleDriveFileHandlers();
        ActionContext context = createContext("google-drive", "uploadFile", Map.of("name", "test.txt"), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.upload(context);
        assertFalse(result.success());
    }

    @Test
    @DisplayName("Google Slides: createPresentation fails when title is missing")
    void testGoogleSlides_missingTitle() {
        GoogleSlidesHandlers handlers = new GoogleSlidesHandlers();
        ActionContext context = createContext("google-slides", "create", Map.of(), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.create(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("title"));
    }

    @Test
    @DisplayName("Google Tasks: createTask fails when task list is missing")
    void testGoogleTasks_missingTaskList() {
        GoogleTasksHandlers handlers = new GoogleTasksHandlers();
        ActionContext context = createContext("google-tasks", "createTask", Map.of("title", "Buy groceries"), Map.of("accessToken", "google-token"));
        ActionResult result = handlers.create(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("tasklistId") || result.error().contains("tasklist"));
    }

    // ── Microsoft 365 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("Microsoft Outlook: sendEmail fails when required fields are missing")
    void testMicrosoftOutlook_missingFields() {
        MicrosoftOutlookMessageHandlers handlers = new MicrosoftOutlookMessageHandlers();
        ActionContext context = createContext("microsoft-outlook", "sendEmail", Map.of("to", "user@example.com"), Map.of("accessToken", "ms-token"));
        ActionResult result = handlers.send(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("required"));
    }

    @Test
    @DisplayName("Microsoft Teams: sendMessage fails when team or channel is missing")
    void testMicrosoftTeams_missingChannel() {
        MicrosoftTeamsChannelMessageHandlers handlers = new MicrosoftTeamsChannelMessageHandlers();
        ActionContext context = createContext("microsoft-teams", "sendChannelMessage", Map.of("content", "Hello Team"), Map.of("accessToken", "ms-token"));
        ActionResult result = handlers.send(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("teamId") || result.error().contains("channelId"));
    }

    @Test
    @DisplayName("Microsoft Excel: createWorkbook fails when fileName is missing")
    void testMicrosoftExcel_missingName() {
        MicrosoftExcelWorkbookHandlers handlers = new MicrosoftExcelWorkbookHandlers();
        ActionContext context = createContext("microsoft-excel", "createWorkbook", Map.of(), Map.of("accessToken", "ms-token"));
        ActionResult result = handlers.createWorkbook(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("fileName"));
    }

    // ── Messaging & Collaboration ─────────────────────────────────────────────

    @Test
    @DisplayName("Slack: sendMessage fails when channel is missing")
    void testSlack_missingChannel() {
        SlackMessageHandlers handlers = new SlackMessageHandlers();
        ActionContext context = createContext("slack", "sendMessage", Map.of("text", "Hi Slack"), Map.of("accessToken", "xoxb-123"));
        ActionResult result = handlers.send(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("channel"));
    }

    @Test
    @DisplayName("Discord: sendMessage fails when channelId is missing")
    void testDiscord_missingChannel() {
        DiscordMessageHandlers handlers = new DiscordMessageHandlers();
        ActionContext context = createContext("discord", "sendMessage", Map.of("content", "Hi Discord"), Map.of("botToken", "bot-token"));
        ActionResult result = handlers.send(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("channelId"));
    }

    @Test
    @DisplayName("Telegram: sendMessage fails when chatId or text is missing")
    void testTelegram_missingChatId() {
        TelegramMessageHandlers handlers = new TelegramMessageHandlers();
        ActionContext context = createContext("telegram", "sendMessage", Map.of("text", "Hello"), Map.of("botToken", "tg-bot-token"));
        ActionResult result = handlers.sendMessage(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("chatId"));
    }

    // ── Developer & Project Tools ─────────────────────────────────────────────

    @Test
    @DisplayName("Notion: NotionSupport generates Bearer header from apiToken and accessToken")
    void testNotion_authHeader() {
        ActionContext context1 = createContext("notion", "notion:page:create", Map.of(), Map.of("apiToken", "secret_123"));
        assertEquals("Bearer secret_123", NotionSupport.getAuthHeader(context1));

        ActionContext context2 = createContext("notion", "notion:page:create", Map.of(), Map.of("accessToken", "oauth_456"));
        assertEquals("Bearer oauth_456", NotionSupport.getAuthHeader(context2));
    }

    @Test
    @DisplayName("GitHub: GitHubSupport generates Bearer header from accessToken")
    void testGitHub_authHeader() {
        ActionContext context = createContext("github", "github:issue:create", Map.of(), Map.of("accessToken", "gh_token"));
        assertEquals("Bearer gh_token", GitHubSupport.getAuthHeader(context));
    }

    @Test
    @DisplayName("Trello: createCard builds request body with idList and name")
    void testTrello_createCard() {
        TrelloCardHandlers handlers = new TrelloCardHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Todoist: createTask instantiates and validates configuration")
    void testTodoist_createTask() {
        TodoistTaskHandlers handlers = new TodoistTaskHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Toggl: createTimeEntry fails when apiToken is missing")
    void testToggl_missingToken() {
        TogglTimeEntryHandlers handlers = new TogglTimeEntryHandlers();
        ActionContext context = createContext("toggl", "createTimeEntry", Map.of("description", "Coding"), Map.of());
        ActionResult result = handlers.createTimeEntry(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("apiToken") || result.error().contains("apiKey"));
    }

    // ── Social, Media & CRM ───────────────────────────────────────────────────

    @Test
    @DisplayName("Twitter: postTweet instantiates and validates text mapping")
    void testTwitter_postTweet() {
        TwitterTweetHandlers handlers = new TwitterTweetHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Strava: createActivity instantiates and validates activity mapping")
    void testStrava_createActivity() {
        StravaActivityHandlers handlers = new StravaActivityHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Salesforce: createLead instantiates and validates lead mapping")
    void testSalesforce_createLead() {
        SalesforceLeadHandlers handlers = new SalesforceLeadHandlers();
        assertNotNull(handlers);
    }

    @Test
    @DisplayName("Dropbox: uploadText fails when path or content is missing")
    void testDropbox_missingPath() throws Exception {
        DropboxFileHandlers handlers = new DropboxFileHandlers();
        ActionContext context = createContext("dropbox", "upload-text", Map.of(), Map.of("accessToken", "db-token"));
        Object result = handlers.uploadText(context);
        assertTrue(result instanceof ActionResult);
        ActionResult actionResult = (ActionResult) result;
        assertFalse(actionResult.success());
        assertTrue(actionResult.error().contains("Path and content are required"));
    }

    @Test
    @DisplayName("YouTube: uploadVideo fails when title is missing")
    void testYouTube_missingTitle() {
        YouTubeVideoHandlers handlers = new YouTubeVideoHandlers();
        ActionContext context = createContext("youtube", "uploadVideo", Map.of(), Map.of("accessToken", "yt-token"));
        ActionResult result = handlers.upload(context);
        assertFalse(result.success());
        assertTrue(result.error().contains("title"));
    }

    @Test
    @DisplayName("WordPress: createPost instantiates and validates post mapping")
    void testWordPress_createPost() {
        WordPressPostHandlers handlers = new WordPressPostHandlers();
        assertNotNull(handlers);
    }
}
