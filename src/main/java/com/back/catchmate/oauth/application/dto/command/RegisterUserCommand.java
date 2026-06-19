package com.back.catchmate.oauth.application.dto.command;

import java.time.LocalDate;

public record RegisterUserCommand(
        String provider,
        String providerIdWithProvider,
        String email,
        String nickName,
        Character gender,
        LocalDate birthDate,
        Long favoriteClubId,
        String profileImageUrl,
        String watchStyle
) {
}
