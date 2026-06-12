package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.Enroll;
import java.time.LocalDateTime;

public record EnrollResponse(
        Long enrollId,
        String description,
        boolean newEnroll,
        LocalDateTime requestDate,
        ApplicantResponse applicant
) {
    public static EnrollResponse from(Enroll enroll) {
        return new EnrollResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.isNewEnroll(),
                enroll.getRequestedAt(),
                ApplicantResponse.from(enroll.getUser())
        );
    }
}
