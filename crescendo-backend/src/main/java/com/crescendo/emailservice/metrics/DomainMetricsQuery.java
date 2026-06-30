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

    @Id
    @Column(name = "domainId", nullable = false)
    private UUID domainId;

    @Id
    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "sentCount", nullable = false)
    private long sentCount = 0;

    @Column(name = "bounceCount", nullable = false)
    private long bounceCount = 0;

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

    public long getBounceCount() { return bounceCount; }
    public void setBounceCount(long bounceCount) { this.bounceCount = bounceCount; }

    public long getSpamCount() { return spamCount; }
    public void setSpamCount(long spamCount) { this.spamCount = spamCount; }

    public void incrementSent() { this.sentCount++; }
    public void incrementBounce() { this.bounceCount++; }
    public void incrementSpam() { this.spamCount++; }

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
