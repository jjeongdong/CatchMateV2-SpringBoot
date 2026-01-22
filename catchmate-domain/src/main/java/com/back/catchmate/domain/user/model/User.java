package com.back.catchmate.domain.user.model;

import com.back.catchmate.domain.club.model.Club;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
public class User {
    private Long id;
    private String email;
    private Provider provider;
    private String providerId;
    private Character gender;
    private String nickName;
    private LocalDate birthDate;
    private String watchStyle;
    private String profileImageUrl;
    private Character allAlarm;
    private Character chatAlarm;
    private Character enrollAlarm;
    private Character eventAlarm;
    private String fcmToken;
    private Authority authority;
    private boolean isReported;
    private Club club;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static User createUser(Provider provider,
                                  String providerId,
                                  String email,
                                  String nickName,
                                  Character gender,
                                  LocalDate birthDate,
                                  Club favoriteClub,
                                  String profileImageUrl,
                                  String fcmToken,
                                  String watchStyle) {
        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .nickName(nickName)
                .gender(gender)
                .birthDate(birthDate)
                .club(favoriteClub)
                .profileImageUrl(profileImageUrl)
                .allAlarm('Y')
                .chatAlarm('Y')
                .enrollAlarm('Y')
                .eventAlarm('Y')
                .fcmToken(fcmToken)
                .authority(Authority.ROLE_USER)
                .isReported(false)
                .watchStyle(watchStyle)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private boolean isNewFcmToken(String fcmToken) {
        return !Objects.equals(this.fcmToken, fcmToken);
    }

    public void updateFcmToken(String fcmToken) {
        if (isNewFcmToken(fcmToken)) {
            this.fcmToken = fcmToken;
        }
    }
}
