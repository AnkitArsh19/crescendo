package com.crescendo.emailservice.metrics;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "daily_domain_metrics_query")
@IdClass(DomainMetricsQuery.DomainMetricsQueryId.class)
public class DomainMetricsQuery {

    /** Gmail/Yahoo bulk-sender thresholds — used as risk reference lines in the metrics dashboard. */
    public static final double BOUNCE_RISK_THRESHOLD    = 0.04;  // 4%
    public static final double COMPLAINT_RISK_THRESHOLD = 0.001; // 0.1%

    @Id
    @Column(name = "domainId", nullable = false)
    private UUID domainId;

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "sentCount", nullable = false)
    private long sentCount = 0;

    /** Transient (soft) bounces — temporary delivery failures, e.g. mailbox full. */
    @Column(name = "transientBounceCount", nullable = false)
    private long transientBounceCount = 0;

    /** Permanent (hard) bounces — undeliverable address, blacklisted domain. */
    @Column(name = "permanentBounceCount", nullable = false)
    private long permanentBounceCount = 0;

    /** Bounce type could not be determined from provider response. */
    @Column(name = "undeterminedBounceCount", nullable = false)
    private long undeterminedBounceCount = 0;

    @Column(name = "spamCount", nullable = false)
    private long spamCount = 0;

    public DomainMetricsQuery() {
    }

    public DomainMetricsQuery(UUID domainId, LocalDate date) {
        this.domainId = domainId;
        this.date = date;
    }

    public UUID getDomainId() { return domainId; }
    public void setDomainId(UUID domainId) { this.domainId = domainId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public long getSentCount() { return sentCount; }
    public void setSentCount(long sentCount) { this.sentCount = sentCount; }

    public long getTransientBounceCount() { return transientBounceCount; }
    public void setTransientBounceCount(long c) { this.transientBounceCount = c; }

    public long getPermanentBounceCount() { return permanentBounceCount; }
    public void setPermanentBounceCount(long c) { this.permanentBounceCount = c; }

    public long getUndeterminedBounceCount() { return undeterminedBounceCount; }
    public void setUndeterminedBounceCount(long c) { this.undeterminedBounceCount = c; }

    /** Total bounces across all sub-types. */
    public long getTotalBounceCount() { return transientBounceCount + permanentBounceCount + undeterminedBounceCount; }

    public long getSpamCount() { return spamCount; }
    public void setSpamCount(long spamCount) { this.spamCount = spamCount; }

    public void incrementSent()                { this.sentCount++; }
    public void incrementTransientBounce()     { this.transientBounceCount++; }
    public void incrementPermanentBounce()     { this.permanentBounceCount++; }
    public void incrementUndeterminedBounce()  { this.undeterminedBounceCount++; }
    public void incrementSpam()                { this.spamCount++; }

    public static class DomainMetricsQueryId implements Serializable {
        private UUID domainId;
        private LocalDate date;

        public DomainMetricsQueryId() {}

        public DomainMetricsQueryId(UUID domainId, LocalDate date) {
            this.domainId = domainId;
            this.date = date;
        }

        public UUID getDomainId() { return domainId; }
        public void setDomainId(UUID domainId) { this.domainId = domainId; }

        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DomainMetricsQueryId that = (DomainMetricsQueryId) o;
            return Objects.equals(domainId, that.domainId) && Objects.equals(date, that.date);
        }

        @Override
        public int hashCode() {
            return Objects.hash(domainId, date);
        }
    }
}
