package com.crescendo.emailservice.domain;

import com.crescendo.enums.DomainStatus;
import com.crescendo.enums.DomainSendReadiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DomainRepository extends JpaRepository<Domain, UUID> {

    List<Domain> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<Domain> findByIdAndUser_Id(UUID id, UUID userId);

    @Query("SELECT d FROM Domain d WHERE d.domainName.value = :domainName AND d.user.id = :userId")
    Optional<Domain> findByDomainNameAndUserId(String domainName, UUID userId);

    @Query("SELECT d FROM Domain d WHERE d.domainName.value = :domainName AND d.status = :status")
    Optional<Domain> findByDomainNameAndStatus(String domainName, DomainStatus status);

    List<Domain> findBySendReadiness(DomainSendReadiness readiness);

    boolean existsByEmailProviderConnectionId(UUID emailProviderConnectionId);
}
