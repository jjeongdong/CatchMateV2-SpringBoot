package com.back.catchmate.orchestration.user.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.user.enums.Provider;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
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
