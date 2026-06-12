package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.Enroll;
import java.time.LocalDateTime;

public record EnrollApplicantResponse(
        Long enrollId,
        String description,
        LocalDateTime requestDate,
        boolean newEnroll,
        ApplicantResponse applicantResponse
) {
    public static EnrollApplicantResponse from(Enroll enroll) {
        return new EnrollApplicantResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                true,
                ApplicantResponse.from(enroll.getUser())
        );
    }
}
