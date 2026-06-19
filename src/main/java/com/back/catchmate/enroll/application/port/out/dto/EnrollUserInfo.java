package com.back.catchmate.enroll.application.port.out.dto;

import java.time.LocalDate;

public record EnrollUserInfo(
        Long userId,
        Long clubId,
        String nickName,
        String email,
        String profileImageUrl,
        Character gender,
        LocalDate birthDate,
        String watchStyle,
        String authority
) {
}
