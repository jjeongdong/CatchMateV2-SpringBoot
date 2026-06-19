package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDateTime;

public record EnrollResponse(
        Long enrollId,
        String description,
        boolean newEnroll,
        LocalDateTime requestDate,
        ApplicantResponse applicant
) {
}
