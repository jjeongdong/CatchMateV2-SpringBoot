package com.back.catchmate.enroll.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class EnrollCountResponse {
    private long count;

    public static EnrollCountResponse of(long count) {
        return EnrollCountResponse.builder()
                .count(count)
                .build();
    }
}
