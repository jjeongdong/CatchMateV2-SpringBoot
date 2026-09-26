package com.back.catchmate.auth.application.dto.command;

import java.time.LocalDate;

public record SignUpCommand(
        String signupToken,
        Character gender,
        String nickName,
        LocalDate birthDate,
        Long favoriteClubId,
        String watchStyle) {}
