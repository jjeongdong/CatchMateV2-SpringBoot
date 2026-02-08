package com.back.catchmate.orchestration.enroll.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EnrollCreateResponse {
    private Long enrollId;
    private LocalDateTime requestAt;

    public static EnrollCreateResponse of(Long enrollId) {
        return EnrollCreateResponse.builder()
                .enrollId(enrollId)
                .requestAt(LocalDateTime.now())
                .build();
    }
}
