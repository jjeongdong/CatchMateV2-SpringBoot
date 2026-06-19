package com.back.catchmate.enroll.application.dto.response;

import com.back.catchmate.enroll.domain.model.AcceptStatus;

import java.time.LocalDateTime;

public record EnrollDetailResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        EnrollApplicantDetailView applicant,
        EnrollBoardSummary boardResponse
) {
}
