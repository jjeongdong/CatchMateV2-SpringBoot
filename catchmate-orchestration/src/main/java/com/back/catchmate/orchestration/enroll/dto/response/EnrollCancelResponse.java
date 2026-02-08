package com.back.catchmate.orchestration.enroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollCancelResponse {
    private Long enrollId;
    private LocalDateTime deletedAt;

    public static EnrollCancelResponse of(Long enrollId) {
        return EnrollCancelResponse.builder()
                .enrollId(enrollId)
                .deletedAt(LocalDateTime.now())
                .build();
    }
}
