package com.back.catchmate.report.adapter.out.persistence.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.report.domain.model.Report;
import com.back.catchmate.report.domain.model.ReportReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "reports")
public class ReportEntity extends BaseTimeEntity {
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

    public static ReportEntity from(Report report) {
        return ReportEntity.builder()
                .id(report.getId())
                .reporterId(report.getReporterId())
                .reportedUserId(report.getReportedUserId())
                .reason(report.getReason())
                .description(report.getDescription())
                .completed(report.isCompleted())
                .build();
    }

    public Report toDomain() {
        return Report.builder()
                .id(this.id)
                .reporterId(this.reporterId)
                .reportedUserId(this.reportedUserId)
                .reason(this.reason)
                .description(this.description)
                .createdAt(this.getCreatedAt())
                .completed(this.completed)
                .build();
    }
}
