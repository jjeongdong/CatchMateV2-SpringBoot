package com.back.catchmate.report.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.report.domain.exception.ReportSelfNotAllowedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reports")
public class Report extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean completed;

    private Report(Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.reason = reason;
        this.description = description;
        this.completed = false;
    }

    public static Report create(Long reporterId, Long reportedUserId, ReportReason reason, String description) {
        if (reporterId.equals(reportedUserId)) {
            throw new ReportSelfNotAllowedException();
        }
        return new Report(reporterId, reportedUserId, reason, description);
    }

    public void process() {
        this.completed = true;
    }
}
