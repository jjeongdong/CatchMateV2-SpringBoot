package com.back.catchmate.application.user.dto;

import com.back.catchmate.domain.user.model.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
public class UserRegisterCommand {
    private final Provider provider;
    private final String providerIdWithProvider;
    private final String email;
    private final String profileImageUrl;
    private final String fcmToken;
    private final Character gender;
    private final String nickName;
    private final LocalDate birthDate;
    private final Long favoriteClubId;
    private final String watchStyle;
}
