package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDateTime;

public record EnrollApplicantResponse(
        Long enrollId,
        String description,
        LocalDateTime requestDate,
        boolean newEnroll,
        ApplicantResponse applicantResponse
) {
}
