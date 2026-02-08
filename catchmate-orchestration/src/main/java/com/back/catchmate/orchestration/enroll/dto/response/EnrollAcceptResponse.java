package com.back.catchmate.orchestration.enroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EnrollAcceptResponse {
    private Long enrollId;
    private String message;

    public static EnrollAcceptResponse of(Long enrollId) {
        return EnrollAcceptResponse.builder()
                .enrollId(enrollId)
                .message("직관 신청을 수락했습니다.")
                .build();
    }
}
