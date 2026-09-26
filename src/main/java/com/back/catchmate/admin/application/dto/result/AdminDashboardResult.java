package com.back.catchmate.admin.application.dto.result;

import java.util.Map;

public record AdminDashboardResult(
        long totalUserCount,
        GenderRatio genderRatio,
        long totalBoardCount,
        Map<String, Long> userCountByClub,
        Map<String, Long> userCountByWatchStyle,
        long totalReportCount,
        long pendingReportCount,
        long totalInquiryCount,
        long waitingInquiryCount) {

    public record GenderRatio(long maleCount, long femaleCount) {}
}
