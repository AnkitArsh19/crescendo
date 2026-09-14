package com.crescendo.apps.telegram.repository;

import com.crescendo.apps.telegram.entity.TelegramLinkingNonce;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelegramLinkingNonceRepository extends JpaRepository<TelegramLinkingNonce, UUID> {
    Optional<TelegramLinkingNonce> findByToken(String token);
    Optional<TelegramLinkingNonce> findByTokenAndStatus(String token, String status);
}
