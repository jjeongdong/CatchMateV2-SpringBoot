package com.back.catchmate.enroll.application.dto.response;

import java.time.LocalDate;

public record EnrollApplicantDetailView(
        Long userId,
        String nickName,
        String email,
        String profileImageUrl,
        char gender,
        LocalDate birthDate,
        String watchStyle,
        EnrollClubView club,
        String authority
) {
}
