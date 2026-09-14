package com.crescendo.apps.telegram.repository;

import com.crescendo.apps.telegram.entity.UserTelegramChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserTelegramChatRepository extends JpaRepository<UserTelegramChat, UUID> {
    List<UserTelegramChat> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);
    List<UserTelegramChat> findAllByTelegramUserId(Long telegramUserId);
    Optional<UserTelegramChat> findByUserIdAndChatId(UUID userId, String chatId);
    void deleteByUserIdAndChatId(UUID userId, String chatId);
}
