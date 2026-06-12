package com.back.catchmate.oauth.application.dto.command;

import java.time.LocalDate;

public record SignUpCommand(
        String signupToken,
        Character gender,
        String nickName,
        LocalDate birthDate,
        Long favoriteClubId,
        String watchStyle
) {
}
