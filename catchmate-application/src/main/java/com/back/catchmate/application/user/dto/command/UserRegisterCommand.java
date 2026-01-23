package com.back.catchmate.application.user.dto.command;

import com.back.catchmate.domain.user.model.Provider;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserRegisterCommand {
    private Provider provider;
    private String providerIdWithProvider;
    private String email;
    private String profileImageUrl;
    private String fcmToken;
    private Character gender;
    private String nickName;
    private LocalDate birthDate;
    private Long favoriteClubId;
    private String watchStyle;
}
