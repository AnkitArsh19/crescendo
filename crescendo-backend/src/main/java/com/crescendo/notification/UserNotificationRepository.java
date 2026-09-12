package com.crescendo.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    boolean existsByUserIdAndTypeAndTitleAndIsReadFalse(UUID userId, NotificationType type, String title);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.userId = :userId AND n.id IN :ids")
    int markReadByIds(@Param("userId") UUID userId, @Param("ids") List<UUID> ids);

    @Modifying
    @Query("UPDATE UserNotification n SET n.isRead = true WHERE n.userId = :userId")
    int markAllReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.createdAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId")
    int deleteAllByUserId(@Param("userId") UUID userId);
}
