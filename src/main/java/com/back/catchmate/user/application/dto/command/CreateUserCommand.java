package com.back.catchmate.user.application.dto.command;

import java.time.LocalDate;

public record CreateUserCommand(
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
