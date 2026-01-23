package com.back.catchmate.domain.user.model;

import com.back.catchmate.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import user.enums.AlarmType;
import user.enums.Provider;

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

    public void updateProfile(String nickName, String watchStyle, Club club, String profileImageUrl) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (watchStyle != null) {
            this.watchStyle = watchStyle;
        }
        if (club != null) {
            this.club = club;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updateAlarm(AlarmType alarmType, boolean isEnabled) {
        char status = isEnabled ? 'Y' : 'N';

        if (alarmType == AlarmType.ALL) {
            this.allAlarm = status;
            this.chatAlarm = status;
            this.enrollAlarm = status;
            this.eventAlarm = status;
            return;
        }

        switch (alarmType) {
            case CHAT -> this.chatAlarm = status;
            case ENROLL -> this.enrollAlarm = status;
            case EVENT -> this.eventAlarm = status;
        }
    }

    public void deleteFcmToken() {
        this.fcmToken = null;
    }
}
