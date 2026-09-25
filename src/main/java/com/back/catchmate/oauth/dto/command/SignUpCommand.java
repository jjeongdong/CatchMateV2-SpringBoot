package com.back.catchmate.oauth.dto.command;

import java.time.LocalDate;

public record SignUpCommand(
        String signupToken,
        Character gender,
        String nickName,
        LocalDate birthDate,
        Long favoriteClubId,
        String watchStyle) {}
