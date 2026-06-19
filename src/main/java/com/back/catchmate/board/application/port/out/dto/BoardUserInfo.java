package com.back.catchmate.board.application.port.out.dto;

import java.time.LocalDate;

public record BoardUserInfo(
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
