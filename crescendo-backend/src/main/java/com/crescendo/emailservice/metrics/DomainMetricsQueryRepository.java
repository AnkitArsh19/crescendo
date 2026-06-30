package com.crescendo.emailservice.metrics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface DomainMetricsQueryRepository extends JpaRepository<DomainMetricsQuery, DomainMetricsQuery.DomainMetricsQueryId> {
    List<DomainMetricsQuery> findAllByDomainIdAndDateBetweenOrderByDateAsc(UUID domainId, LocalDate startDate, LocalDate endDate);
}
