package com.back.catchmate.application.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUserCount;
    private GenderRatio genderRatio;
    private long totalBoardCount;
    private Map<String, Long> userCountByClub; // 구단명 : 인원수
    private Map<String, Long> userCountByWatchStyle; // 응원스타일 : 인원수
    private long totalReportCount;
    private long totalInquiryCount;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GenderRatio {
        private long maleCount;
        private long femaleCount;

        public static GenderRatio of(long maleCount, long femaleCount) {
            return GenderRatio.builder()
                    .maleCount(maleCount)
                    .femaleCount(femaleCount)
                    .build();
        }
    }

    public static AdminDashboardResponse of(long totalUserCount,
                                            GenderRatio genderRatio,
                                            long totalBoardCount,
                                            Map<String, Long> userCountByClub,
                                            Map<String, Long> userCountByWatchStyle,
                                            long totalReportCount,
                                            long totalInquiryCount) {
        return AdminDashboardResponse.builder()
                .totalUserCount(totalUserCount)
                .genderRatio(genderRatio)
                .totalBoardCount(totalBoardCount)
                .userCountByClub(userCountByClub)
                .userCountByWatchStyle(userCountByWatchStyle)
                .totalReportCount(totalReportCount)
                .totalInquiryCount(totalInquiryCount)
                .build();
    }
}
