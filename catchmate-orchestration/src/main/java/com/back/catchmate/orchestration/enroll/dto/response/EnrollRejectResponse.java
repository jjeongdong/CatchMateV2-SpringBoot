package com.back.catchmate.orchestration.enroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EnrollRejectResponse {
    private Long enrollId;
    private String message;

    public static EnrollRejectResponse of(Long enrollId) {
        return EnrollRejectResponse.builder()
                .enrollId(enrollId)
                .message("직관 신청을 거절했습니다.")
                .build();
    }
}
