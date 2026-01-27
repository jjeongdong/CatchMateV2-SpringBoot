package com.back.catchmate.application.admin.dto.response;

import com.back.catchmate.domain.report.model.Report;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminReportDetailResponse {
    private Long reportId;
    
    // 신고자 정보
    private Long reporterId;
    private String reporterNickname;
    private String reporterEmail;
    private String reporterProfileImage;

    // 신고 대상자 정보
    private Long reportedUserId;
    private String reportedUserNickname;
    private String reportedUserEmail;
    private String reportedUserProfileImage;

    private String reason;      // 신고 사유
    private String description; // 상세 내용
    private LocalDateTime createdAt;

    public static AdminReportDetailResponse from(Report report) {
        return AdminReportDetailResponse.builder()
                .reportId(report.getId())
                .reporterId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickName())
                .reporterEmail(report.getReporter().getEmail())
                .reporterProfileImage(report.getReporter().getProfileImageUrl())
                .reportedUserId(report.getReportedUser().getId())
                .reportedUserNickname(report.getReportedUser().getNickName())
                .reportedUserEmail(report.getReportedUser().getEmail())
                .reportedUserProfileImage(report.getReportedUser().getProfileImageUrl())
                .reason(report.getReason().name())
                .description(report.getDescription())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
