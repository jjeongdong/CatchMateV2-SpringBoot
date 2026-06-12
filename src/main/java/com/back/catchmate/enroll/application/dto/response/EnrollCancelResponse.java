package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDateTime;

public record EnrollCancelResponse(
        Long enrollId,
        LocalDateTime deletedAt
) {
    public static EnrollCancelResponse of(Long enrollId) {
        return new EnrollCancelResponse(
                enrollId,
                LocalDateTime.now()
        );
    }
}
