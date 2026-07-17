package com.crescendo.storage.storage_command;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface UploadedFile_commandRepository extends JpaRepository<UploadedFile_command, String> {

    @Query("SELECT f FROM UploadedFile_command f WHERE f.status = 'PENDING' AND f.createdAt < :threshold")
    List<UploadedFile_command> findPendingBefore(Instant threshold);

    @Query("SELECT f FROM UploadedFile_command f WHERE f.consumptionModel = 'RELAY' AND f.createdAt < :threshold AND f.status != 'CONSUMED'")
    List<UploadedFile_command> findStaleRelaysBefore(Instant threshold);

    @Query("SELECT COALESCE(SUM(f.sizeBytes), 0) FROM UploadedFile_command f WHERE f.userId = :userId AND f.status != :status")
    Long sumBytesByUserIdAndStatusNot(java.util.UUID userId, FileStatus status);
}
