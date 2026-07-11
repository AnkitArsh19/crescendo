package com.crescendo.emailservice.dmarc;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DmarcAlignmentReportRepository extends JpaRepository<DmarcAlignmentReport, UUID> {
    List<DmarcAlignmentReport> findByDomainIdOrderByCreatedAtDesc(UUID domainId);
}
