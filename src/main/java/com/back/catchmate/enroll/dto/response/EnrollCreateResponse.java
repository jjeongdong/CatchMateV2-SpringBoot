package com.back.catchmate.enroll.dto.response;

import java.time.LocalDateTime;

public record EnrollCreateResponse(
        Long enrollId,
        LocalDateTime requestAt
) {
    public static EnrollCreateResponse of(Long enrollId) {
        return new EnrollCreateResponse(
                enrollId,
                LocalDateTime.now()
        );
    }
}
