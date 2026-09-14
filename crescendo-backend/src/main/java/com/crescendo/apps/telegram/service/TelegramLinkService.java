package com.crescendo.apps.telegram.service;

import com.crescendo.admin.PlatformKey;
import com.crescendo.admin.PlatformKeyRepository;
import com.crescendo.apps.telegram.entity.TelegramLinkingNonce;
import com.crescendo.apps.telegram.entity.UserTelegramChat;
import com.crescendo.apps.telegram.repository.TelegramLinkingNonceRepository;
import com.crescendo.apps.telegram.repository.UserTelegramChatRepository;
import com.crescendo.connections.connections_command.Connections_command;
import com.crescendo.connections.connections_command.Connections_commandRepository;
import com.crescendo.connections.security.ConnectionCredentialsCryptoService;
import com.crescendo.enums.ConnectionStatus;
import com.crescendo.shared.domain.valueobject.AppKey;
import com.crescendo.user.user_command.User_command;
import com.crescendo.user.user_command.User_commandRepository;
import com.crescendo.connections.connections_query.Connections_query;
import com.crescendo.connections.connections_query.Connections_queryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class TelegramLinkService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramLinkService.class);
    private static final String TELEGRAM_API = "https://api.telegram.org";

    private final TelegramLinkingNonceRepository nonceRepo;
    private final UserTelegramChatRepository userChatRepo;
    private final Connections_commandRepository connectionsRepo;
    private final Connections_queryRepository connectionQueryRepo;
    private final User_commandRepository userRepo;
    private final PlatformKeyRepository platformKeyRepo;
    private final ConnectionCredentialsCryptoService cryptoService;
    private final ObjectMapper objectMapper;

    @Value("${telegram.bot.token:}")
    private String envBotToken;

    @Value("${telegram.bot.username:crescendo_app_bot}")
    private String botUsername;

    public TelegramLinkService(TelegramLinkingNonceRepository nonceRepo,
                               UserTelegramChatRepository userChatRepo,
                               Connections_commandRepository connectionsRepo,
                               Connections_queryRepository connectionQueryRepo,
                               User_commandRepository userRepo,
                               PlatformKeyRepository platformKeyRepo,
                               ConnectionCredentialsCryptoService cryptoService,
                               ObjectMapper objectMapper) {
        this.nonceRepo = nonceRepo;
        this.userChatRepo = userChatRepo;
        this.connectionsRepo = connectionsRepo;
        this.connectionQueryRepo = connectionQueryRepo;
        this.userRepo = userRepo;
        this.platformKeyRepo = platformKeyRepo;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves the active platform bot token from DB (platform_key table) or environment variable.
     */
    public String resolvePlatformBotToken() {
        try {
            PlatformKey pk = platformKeyRepo.findByAppKeyAndEnabledTrue("telegram").orElse(null);
            if (pk != null && pk.getEncryptedCredentials() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sealed = objectMapper.readValue(pk.getEncryptedCredentials(), Map.class);
                Map<String, Object> opened = cryptoService.open(sealed);
                if (opened != null) {
                    Object t = opened.get("botToken");
                    if (t == null) t = opened.get("apiKey");
                    if (t == null) t = opened.get("token");
                    if (t != null && !t.toString().isBlank()) {
                        return t.toString().trim();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("[telegram-link] Error decrypting platform key for telegram: {}", e.getMessage());
        }

        if (envBotToken != null && !envBotToken.isBlank()) {
            return envBotToken.trim();
        }
        return null;
    }

    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Initiates a 10-minute linking request for the authenticated user.
     */
    @Transactional
    public Map<String, Object> initiateLink(UUID userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(10));

        TelegramLinkingNonce nonce = new TelegramLinkingNonce(UUID.randomUUID(), token, userId, expiresAt);
        nonceRepo.save(nonce);

        String cleanBotUser = (botUsername != null && !botUsername.isBlank())
                ? botUsername.replace("@", "").trim()
                : "crescendo_app_bot";

        String deepLink = "https://t.me/" + cleanBotUser + "?start=" + token;

        logger.info("[telegram-link] Generated link nonce for user {}: token={} expiresAt={}", userId, token, expiresAt);

        return Map.of(
                "token", token,
                "deepLink", deepLink,
                "botUsername", cleanBotUser,
                "expiresAt", expiresAt.toString()
        );
    }

    /**
     * Checks the status of a linking token (polled by frontend).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getLinkStatus(String token) {
        if (token == null || token.isBlank()) {
            return Map.of("status", "INVALID");
        }

        Optional<TelegramLinkingNonce> opt = nonceRepo.findByToken(token.trim());
        if (opt.isEmpty()) {
            return Map.of("status", "NOT_FOUND");
        }

        TelegramLinkingNonce nonce = opt.get();
        if ("PENDING".equals(nonce.getStatus()) && Instant.now().isAfter(nonce.getExpiresAt())) {
            return Map.of("status", "EXPIRED");
        }

        Map<String, Object> res = new HashMap<>();
        res.put("status", nonce.getStatus());
        if ("COMPLETED".equals(nonce.getStatus())) {
            res.put("telegramUserId", nonce.getTelegramUserId());
            res.put("telegramUsername", nonce.getTelegramUsername());
            res.put("completedAt", nonce.getCompletedAt() != null ? nonce.getCompletedAt().toString() : null);
        }
        return res;
    }

    /**
     * Completes account linking when the user presses START in Telegram.
     */
    @Transactional
    public boolean handleStartCommand(String token, Long telegramUserId, String username, String firstName, Long directChatId) {
        if (token == null || token.isBlank()) return false;

        Optional<TelegramLinkingNonce> opt = nonceRepo.findByTokenAndStatus(token.trim(), "PENDING");
        if (opt.isEmpty()) {
            logger.warn("[telegram-link] No pending nonce found for token: {}", token);
            return false;
        }

        TelegramLinkingNonce nonce = opt.get();
        if (Instant.now().isAfter(nonce.getExpiresAt())) {
            nonce.setStatus("EXPIRED");
            nonceRepo.save(nonce);
            logger.warn("[telegram-link] Token {} expired at {}", token, nonce.getExpiresAt());
            return false;
        }

        UUID userId = nonce.getUserId();
        User_command user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            logger.error("[telegram-link] User {} not found for token {}", userId, token);
            return false;
        }

        // 1. Mark nonce as COMPLETED
        nonce.setStatus("COMPLETED");
        nonce.setTelegramUserId(telegramUserId);
        nonce.setTelegramUsername(username);
        nonce.setCompletedAt(Instant.now());
        nonceRepo.save(nonce);

        // 2. Create or update user's Telegram connection
        List<Connections_command> existingConns = connectionsRepo.findByUser_IdOrderByCreatedAtDesc(userId);
        Connections_command tgConn = existingConns.stream()
                .filter(c -> "telegram".equalsIgnoreCase(c.getAppKey()))
                .findFirst()
                .orElse(null);

        String displayName = (username != null && !username.isBlank())
                ? "Telegram (@" + username + ")"
                : "Telegram (" + telegramUserId + ")";

        String platformToken = resolvePlatformBotToken();
        Map<String, Object> creds = new HashMap<>();
        creds.put("type", "PLATFORM_LINKED");
        creds.put("telegramUserId", telegramUserId);
        creds.put("username", username != null ? username : "");
        creds.put("directChatId", directChatId);
        if (platformToken != null && !platformToken.isBlank()) {
            creds.put("apiKey", platformToken);
            creds.put("botToken", platformToken);
        }
        var sealedCreds = cryptoService.seal(creds);

        if (tgConn == null) {
            tgConn = new Connections_command();
            tgConn.setId(UUID.randomUUID());
            tgConn.setUser(user);
            tgConn.setAppKey(AppKey.of("telegram"));
            tgConn.setName(displayName);
            tgConn.setCredentials(sealedCreds);
            tgConn.setStatus(ConnectionStatus.ACTIVE);
            connectionsRepo.save(tgConn);
            logger.info("[telegram-link] Created new Telegram connection {} for user {}", tgConn.getId(), userId);
        } else {
            tgConn.setName(displayName);
            tgConn.setCredentials(sealedCreds);
            tgConn.setStatus(ConnectionStatus.ACTIVE);
            connectionsRepo.save(tgConn);
            logger.info("[telegram-link] Updated existing Telegram connection {} for user {}", tgConn.getId(), userId);
        }

        // Project to read model for UI queries
        Connections_query queryConn = connectionQueryRepo.findById(tgConn.getId())
                .orElse(new Connections_query(tgConn.getId(), userId, "telegram", displayName, ConnectionStatus.ACTIVE));
        queryConn.setName(displayName);
        queryConn.setStatus(ConnectionStatus.ACTIVE);
        connectionQueryRepo.save(queryConn);

        // 3. Register direct chat in user_telegram_chat
        String directChatStr = String.valueOf(directChatId);
        UserTelegramChat directChat = userChatRepo.findByUserIdAndChatId(userId, directChatStr).orElse(null);
        String directTitle = (firstName != null && !firstName.isBlank() ? firstName : "")
                + (username != null && !username.isBlank() ? " (@" + username + ")" : " (" + directChatStr + ")");
        if (directTitle.isBlank()) directTitle = "Direct Chat (" + directChatStr + ")";

        if (directChat == null) {
            directChat = new UserTelegramChat(UUID.randomUUID(), userId, telegramUserId, directChatStr, directTitle.trim(), "private", "member");
        } else {
            directChat.setTitle(directTitle.trim());
            directChat.setTelegramUserId(telegramUserId);
            directChat.setBotStatus("member");
        }
        userChatRepo.save(directChat);

        // 4. Send confirmation message in Telegram
        sendConfirmationMessage(directChatId, firstName);

        logger.info("[telegram-link] Successfully linked Telegram user {} (@{}) to Crescendo user {}", telegramUserId, username, userId);
        return true;
    }

    /**
     * Responds to users who send a bare /start or greeting directly without a deep-link nonce.
     */
    public void handlePlainStartCommand(Long telegramUserId, Long directChatId, String firstName) {
        String token = resolvePlatformBotToken();
        if (token == null || token.isBlank() || directChatId == null) return;

        List<UserTelegramChat> existingChats = telegramUserId != null
                ? userChatRepo.findAllByTelegramUserId(telegramUserId)
                : List.of();

        String cleanBotUsername = (botUsername != null && !botUsername.isBlank())
                ? botUsername.replaceAll("^@", "")
                : "crescendo_app_bot";
        String cleanName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        String text;

        if (!existingChats.isEmpty()) {
            text = "👋 <b>Welcome back, " + escapeHtml(cleanName) + "!</b>\n\n"
                    + "Your Telegram account is already linked to your <b>Crescendo</b> account.\n\n"
                    + "• <b>Direct Alerts:</b> Ready to receive notifications.\n"
                    + "• <b>Channels & Groups:</b> To automate posting, add <a href=\"https://t.me/" + cleanBotUsername + "\">@" + cleanBotUsername + "</a> to your channel or group as an <b>Administrator</b> with <i>Post Messages</i> enabled.\n\n"
                    + "Head back to Crescendo to build or run your workflows!";
        } else {
            text = "👋 <b>Hello " + escapeHtml(cleanName) + "! Welcome to Crescendo.</b>\n\n"
                    + "To link this Telegram account with your Crescendo workspace:\n\n"
                    + "1️⃣ Open <b>Crescendo</b> in your browser or desktop app.\n"
                    + "2️⃣ Go to <b>Connections</b> (or add a Telegram node in your workflow) and select <b>Telegram</b>.\n"
                    + "3️⃣ Click <b>'Open in Telegram to Connect'</b>.\n"
                    + "4️⃣ Tap <b>START</b> in the prompt that appears.\n\n"
                    + "Once linked, you can send automated messages and alerts straight to Telegram!";
        }

        try {
            RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(TELEGRAM_API + "/bot" + token + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", directChatId,
                            "text", text,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("[telegram-link] Failed to send plain /start response to {}: {}", directChatId, e.getMessage());
        }
    }

    /**
     * Resolves Crescendo user IDs that are administrators of the given Telegram chat.
     */
    public Set<UUID> findLinkedUserIdsForChat(String chatId) {
        Set<UUID> matchedUsers = new HashSet<>();
        String token = resolvePlatformBotToken();
        if (token == null || token.isBlank() || chatId == null || chatId.isBlank()) {
            return matchedUsers;
        }

        try {
            Map<String, Object> resp = RestClient.create().get()
                    .uri(TELEGRAM_API + "/bot" + token + "/getChatAdministrators?chat_id=" + chatId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (resp != null && Boolean.TRUE.equals(resp.get("ok"))) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> admins = (List<Map<String, Object>>) resp.get("result");
                if (admins != null) {
                    for (Map<String, Object> admin : admins) {
                        Map<String, Object> userObj = getMap(admin, "user");
                        if (userObj != null) {
                            Long adminTgId = getLong(userObj, "id");
                            if (adminTgId != null) {
                                List<UserTelegramChat> chats = userChatRepo.findAllByTelegramUserId(adminTgId);
                                for (UserTelegramChat uc : chats) {
                                    matchedUsers.add(uc.getUserId());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("[telegram-link] Could not fetch chat administrators for {}: {}", chatId, e.getMessage());
        }
        return matchedUsers;
    }

    /**
     * Handles group & channel member updates (my_chat_member) when bot is added or removed.
     */
    @Transactional
    public void handleChatMemberUpdate(Map<String, Object> myChatMember) {
        if (myChatMember == null) return;

        Map<String, Object> chat = getMap(myChatMember, "chat");
        Map<String, Object> from = getMap(myChatMember, "from");
        Map<String, Object> newMember = getMap(myChatMember, "new_chat_member");

        if (chat == null || newMember == null) return;

        Long telegramUserId = from != null ? getLong(from, "id") : null;
        Long chatIdNum = getLong(chat, "id");
        if (chatIdNum == null) return;

        String chatId = String.valueOf(chatIdNum);
        String chatTitle = getString(chat, "title");
        String chatType = getString(chat, "type");
        if (chatType == null) chatType = "group";
        if (chatTitle == null || chatTitle.isBlank()) {
            chatTitle = chatType.substring(0, 1).toUpperCase() + chatType.substring(1) + " (" + chatId + ")";
        }

        String status = getString(newMember, "status"); // administrator, member, left, kicked

        // Find all Crescendo users linked to this telegramUserId
        Set<UUID> userIds = new HashSet<>();
        if (telegramUserId != null) {
            List<UserTelegramChat> existingUserChats = userChatRepo.findAllByTelegramUserId(telegramUserId);
            for (UserTelegramChat uc : existingUserChats) {
                userIds.add(uc.getUserId());
            }
        }

        // If not found yet, query administrators of the chat (e.g. for channels or groups added earlier)
        if (userIds.isEmpty()) {
            userIds = findLinkedUserIdsForChat(chatId);
        }

        if (userIds.isEmpty()) {
            logger.debug("[telegram-link] Received my_chat_member for unlinked chat {} / telegramUserId: {}", chatId, telegramUserId);
            return;
        }

        boolean isPresent = "administrator".equalsIgnoreCase(status) || "member".equalsIgnoreCase(status);

        for (UUID userId : userIds) {
            if (isPresent) {
                UserTelegramChat c = userChatRepo.findByUserIdAndChatId(userId, chatId).orElse(null);
                if (c == null) {
                    c = new UserTelegramChat(UUID.randomUUID(), userId, telegramUserId, chatId, chatTitle, chatType, status);
                } else {
                    c.setTitle(chatTitle);
                    c.setType(chatType);
                    c.setBotStatus(status);
                    if (telegramUserId != null && c.getTelegramUserId() == null) {
                        c.setTelegramUserId(telegramUserId);
                    }
                }
                userChatRepo.save(c);
                logger.info("[telegram-link] Registered {} '{}' ({}) for Crescendo user {}", chatType, chatTitle, chatId, userId);
            } else {
                userChatRepo.deleteByUserIdAndChatId(userId, chatId);
                logger.info("[telegram-link] Removed {} '{}' ({}) for Crescendo user {} (bot left/kicked)", chatType, chatTitle, chatId, userId);
            }
        }
    }

    /**
     * Handles standard messages or channel posts to capture group/channel chats from linked users.
     */
    @Transactional
    public void handleMessageUpdate(Map<String, Object> message) {
        if (message == null) return;

        Map<String, Object> chat = getMap(message, "chat");
        Map<String, Object> from = getMap(message, "from");
        if (chat == null) return;

        String chatType = getString(chat, "type");
        if ("private".equalsIgnoreCase(chatType)) return; // Direct chats handled in /start

        Long chatIdNum = getLong(chat, "id");
        if (chatIdNum == null) return;
        String chatId = String.valueOf(chatIdNum);
        String chatTitle = getString(chat, "title");
        if (chatTitle == null || chatTitle.isBlank()) {
            chatTitle = (chatType != null ? chatType : "Chat") + " (" + chatId + ")";
        }

        Long telegramUserId = from != null ? getLong(from, "id") : null;

        Set<UUID> userIds = new HashSet<>();
        if (telegramUserId != null) {
            List<UserTelegramChat> existingUserChats = userChatRepo.findAllByTelegramUserId(telegramUserId);
            for (UserTelegramChat uc : existingUserChats) {
                userIds.add(uc.getUserId());
            }
        }

        // If from was null (standard in channel_post) or sender not matched, find admins of the channel/group
        if (userIds.isEmpty()) {
            userIds = findLinkedUserIdsForChat(chatId);
        }

        for (UUID userId : userIds) {
            UserTelegramChat existing = userChatRepo.findByUserIdAndChatId(userId, chatId).orElse(null);
            if (existing == null) {
                UserTelegramChat newChat = new UserTelegramChat(UUID.randomUUID(), userId, telegramUserId, chatId, chatTitle, chatType, "administrator");
                userChatRepo.save(newChat);
                logger.info("[telegram-link] Auto-discovered {} '{}' ({}) for Crescendo user {}", chatType, chatTitle, chatId, userId);
            } else if (!chatTitle.equals(existing.getTitle())) {
                existing.setTitle(chatTitle);
                userChatRepo.save(existing);
            }
        }
    }

    /**
     * Directly adds a Telegram chat (channel, group, or user) for a Crescendo user by username or ID.
     */
    @Transactional
    public UserTelegramChat addChatDirectly(UUID userId, String chatIdOrUsername) {
        if (chatIdOrUsername == null || chatIdOrUsername.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Chat ID or @username is required");
        }
        String token = resolvePlatformBotToken();
        if (token == null || token.isBlank()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Telegram platform bot is not configured");
        }

        String query = chatIdOrUsername.trim();
        if (!query.startsWith("@") && !query.startsWith("-") && !Character.isDigit(query.charAt(0))) {
            query = "@" + query;
        }

        Map<String, Object> chatData;
        try {
            Map<String, Object> resp = RestClient.create().get()
                    .uri(TELEGRAM_API + "/bot" + token + "/getChat?chat_id=" + query)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (resp == null || !Boolean.TRUE.equals(resp.get("ok"))) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Chat not found on Telegram. Please check the @username or chat ID.");
            }
            chatData = getMap(resp, "result");
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Could not find chat on Telegram: " + e.getMessage());
        }

        if (chatData == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "No chat data returned by Telegram");
        }

        Long chatIdNum = getLong(chatData, "id");
        if (chatIdNum == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid chat ID returned by Telegram");
        }
        String resolvedChatId = String.valueOf(chatIdNum);
        String chatType = getString(chatData, "type");
        if (chatType == null) chatType = "channel";
        String chatTitle = getString(chatData, "title");
        if (chatTitle == null || chatTitle.isBlank()) {
            chatTitle = getString(chatData, "username") != null ? "@" + getString(chatData, "username") : resolvedChatId;
        }

        String cleanBotUsername = (botUsername != null && !botUsername.isBlank())
                ? botUsername.replaceAll("^@", "")
                : "crescendo_app_bot";

        // Verify bot status
        String botStatus = "member";
        try {
            Map<String, Object> meResp = RestClient.create().get()
                    .uri(TELEGRAM_API + "/bot" + token + "/getMe")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (meResp != null && Boolean.TRUE.equals(meResp.get("ok"))) {
                Map<String, Object> me = getMap(meResp, "result");
                Long botId = me != null ? getLong(me, "id") : null;
                if (botId != null) {
                    Map<String, Object> memberResp = RestClient.create().get()
                            .uri(TELEGRAM_API + "/bot" + token + "/getChatMember?chat_id=" + resolvedChatId + "&user_id=" + botId)
                            .retrieve()
                            .body(new ParameterizedTypeReference<>() {});
                    if (memberResp != null && Boolean.TRUE.equals(memberResp.get("ok"))) {
                        Map<String, Object> member = getMap(memberResp, "result");
                        botStatus = member != null ? getString(member, "status") : "member";
                        if ("left".equalsIgnoreCase(botStatus) || "kicked".equalsIgnoreCase(botStatus)) {
                            throw new org.springframework.web.server.ResponseStatusException(
                                    org.springframework.http.HttpStatus.BAD_REQUEST,
                                    "The bot @" + cleanBotUsername + " is not a member or administrator of this chat. Please add it first!");
                        }
                    }
                }
            }
        } catch (org.springframework.web.server.ResponseStatusException rse) {
            throw rse;
        } catch (Exception e) {
            logger.warn("[telegram-link] Could not verify bot membership for {}: {}", resolvedChatId, e.getMessage());
        }

        List<UserTelegramChat> existingUserChats = userChatRepo.findAllByUserIdOrderByUpdatedAtDesc(userId);
        Long telegramUserId = existingUserChats.stream()
                .map(UserTelegramChat::getTelegramUserId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

        UserTelegramChat chat = userChatRepo.findByUserIdAndChatId(userId, resolvedChatId).orElse(null);
        if (chat == null) {
            chat = new UserTelegramChat(UUID.randomUUID(), userId, telegramUserId, resolvedChatId, chatTitle, chatType, botStatus);
        } else {
            chat.setTitle(chatTitle);
            chat.setType(chatType);
            chat.setBotStatus(botStatus);
            if (telegramUserId != null && chat.getTelegramUserId() == null) {
                chat.setTelegramUserId(telegramUserId);
            }
        }
        UserTelegramChat saved = userChatRepo.save(chat);
        logger.info("[telegram-link] Manually added {} '{}' ({}) for Crescendo user {}", chatType, chatTitle, resolvedChatId, userId);
        return saved;
    }

    private void sendConfirmationMessage(Long chatId, String firstName) {
        String token = resolvePlatformBotToken();
        if (token == null || token.isBlank()) return;

        String cleanBotUsername = (botUsername != null && !botUsername.isBlank())
                ? botUsername.replaceAll("^@", "")
                : "crescendo_app_bot";
        String cleanName = (firstName != null && !firstName.isBlank()) ? firstName : "there";
        String text = "🎉 <b>Welcome " + escapeHtml(cleanName) + "!</b>\n\n"
                + "Your Telegram account has been linked to <b>Crescendo</b>.\n\n"
                + "• You can now send automated messages and alerts directly to this chat.\n"
                + "• To post to <b>Channels</b> or <b>Groups</b>, simply add <a href=\"https://t.me/" + cleanBotUsername + "\">@" + cleanBotUsername + "</a> as an Administrator with <i>Post Messages</i> permission enabled.\n\n"
                + "Head back to Crescendo to build your workflows!";

        try {
            RestClient.builder()
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .build()
                    .post()
                    .uri(TELEGRAM_API + "/bot" + token + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "parse_mode", "HTML"
                    ))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            logger.warn("[telegram-link] Failed to send confirmation message to {}: {}", chatId, e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> parent, String key) {
        Object val = parent.get(key);
        return (val instanceof Map) ? (Map<String, Object>) val : null;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return (val != null) ? val.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number n) return n.longValue();
        if (val instanceof String s) {
            try { return Long.parseLong(s); } catch (Exception ignored) {}
        }
        return null;
    }
}
