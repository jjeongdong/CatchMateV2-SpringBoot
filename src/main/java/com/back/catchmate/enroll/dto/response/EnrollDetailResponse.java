package com.back.catchmate.enroll.dto.response;

import com.back.catchmate.enroll.entity.AcceptStatus;
import java.time.LocalDateTime;

public record EnrollDetailResponse(
        Long enrollId,
        AcceptStatus acceptStatus,
        String description,
        LocalDateTime requestDate,
        EnrollApplicantDetailView applicant,
        EnrollBoardSummary boardResponse) {}
