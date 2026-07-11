package com.crescendo.emailservice.dmarc;

import com.crescendo.emailservice.domain.Domain;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dmarc_alignment_report")
public class DmarcAlignmentReport {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "domain_id", referencedColumnName = "id", nullable = false)
    private Domain domain;

    @Column(name = "org_name")
    private String orgName;

    @Column(name = "report_id")
    private String reportId;

    @Column(name = "source_ip")
    private String sourceIp;

    @Column(name = "spf_aligned")
    private boolean spfAligned;

    @Column(name = "dkim_aligned")
    private boolean dkimAligned;

    @Column(name = "disposition")
    private String disposition;

    @Column(name = "date_range_begin")
    private Instant dateRangeBegin;

    @Column(name = "date_range_end")
    private Instant dateRangeEnd;

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false)
    private Instant createdAt;

    public DmarcAlignmentReport() {}

    public DmarcAlignmentReport(UUID id, Domain domain, String orgName, String reportId,
                                String sourceIp, boolean spfAligned, boolean dkimAligned,
                                String disposition, Instant dateRangeBegin, Instant dateRangeEnd) {
        this.id = id;
        this.domain = domain;
        this.orgName = orgName;
        this.reportId = reportId;
        this.sourceIp = sourceIp;
        this.spfAligned = spfAligned;
        this.dkimAligned = dkimAligned;
        this.disposition = disposition;
        this.dateRangeBegin = dateRangeBegin;
        this.dateRangeEnd = dateRangeEnd;
    }

    public UUID getId() { return id; }
    public Domain getDomain() { return domain; }
    public String getOrgName() { return orgName; }
    public String getReportId() { return reportId; }
    public String getSourceIp() { return sourceIp; }
    public boolean isSpfAligned() { return spfAligned; }
    public boolean isDkimAligned() { return dkimAligned; }
    public String getDisposition() { return disposition; }
    public Instant getDateRangeBegin() { return dateRangeBegin; }
    public Instant getDateRangeEnd() { return dateRangeEnd; }
    public Instant getCreatedAt() { return createdAt; }
}
