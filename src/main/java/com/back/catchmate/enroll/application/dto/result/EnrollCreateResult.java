package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.enroll.domain.Enroll;
import java.time.LocalDateTime;

public record EnrollCreateResult(Long enrollId, LocalDateTime requestAt) {
    public static EnrollCreateResult from(Enroll enroll) {
        return new EnrollCreateResult(enroll.getId(), enroll.getRequestedAt());
    }
}
