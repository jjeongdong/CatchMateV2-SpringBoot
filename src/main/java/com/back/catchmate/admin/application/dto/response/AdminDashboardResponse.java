package com.back.catchmate.admin.application.dto.response;

import java.util.Map;

public record AdminDashboardResponse(
        long totalUserCount,
        GenderRatio genderRatio,
        long totalBoardCount,
        Map<String, Long> userCountByClub,
        Map<String, Long> userCountByWatchStyle,
        long totalReportCount,
        long pendingReportCount,
        long totalInquiryCount,
        long waitingInquiryCount
) {
    public record GenderRatio(long maleCount, long femaleCount) {
        public static GenderRatio of(long maleCount, long femaleCount) {
            return new GenderRatio(maleCount, femaleCount);
        }
    }

    public static AdminDashboardResponse of(long totalUserCount,
                                            GenderRatio genderRatio,
                                            long totalBoardCount,
                                            Map<String, Long> userCountByClub,
                                            Map<String, Long> userCountByWatchStyle,
                                            long totalReportCount,
                                            long pendingReportCount,
                                            long totalInquiryCount,
                                            long waitingInquiryCount) {
        return new AdminDashboardResponse(
                totalUserCount,
                genderRatio,
                totalBoardCount,
                userCountByClub,
                userCountByWatchStyle,
                totalReportCount,
                pendingReportCount,
                totalInquiryCount,
                waitingInquiryCount
        );
    }
}
