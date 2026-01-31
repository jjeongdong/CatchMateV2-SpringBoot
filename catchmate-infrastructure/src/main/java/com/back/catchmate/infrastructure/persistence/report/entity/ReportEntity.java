package com.back.catchmate.infrastructure.persistence.report.entity;

import com.back.catchmate.domain.report.model.Report;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import report.enums.ReportReason;

@Entity
@Table(name = "reports")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ReportEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id")
    private UserEntity reportedUser;

    @Enumerated(EnumType.STRING)
    private ReportReason reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean completed;

    public static ReportEntity from(Report report) {
        return ReportEntity.builder()
                .id(report.getId())
                .reporter(UserEntity.from(report.getReporter()))
                .reportedUser(UserEntity.from(report.getReportedUser()))
                .reason(report.getReason())
                .description(report.getDescription())
                .completed(report.isCompleted())
                .build();
    }

    public Report toModel() {
        return Report.builder()
                .id(this.id)
                .reporter(this.reporter.toModel())
                .reportedUser(this.reportedUser.toModel())
                .reason(this.reason)
                .description(this.description)
                .createdAt(this.getCreatedAt())
                .completed(this.completed)
                .build();
    }
}
