package com.back.catchmate.user.domain.model;

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
    /** OAuth 인증 제공자 식별자 (KAKAO 등). oauth 컨텍스트의 Provider enum 값이 문자열로 저장된다. */
    private String provider;
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
    private boolean reported;
    private Long clubId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static User createUser(String provider, String providerId, String email, String nickName, Character gender,
                                  LocalDate birthDate, Long favoriteClubId, String profileImageUrl, String fcmToken,
                                  String watchStyle) {
        return User.builder()
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .nickName(nickName)
                .gender(gender)
                .birthDate(birthDate)
                .clubId(favoriteClubId)
                .profileImageUrl(profileImageUrl)
                .allAlarm('Y')
                .chatAlarm('Y')
                .enrollAlarm('Y')
                .eventAlarm('Y')
                .fcmToken(fcmToken)
                .authority(Authority.ROLE_USER)
                .reported(false)
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

    public void updateProfile(String nickName, String watchStyle, Long clubId, String profileImageUrl) {
        if (nickName != null) {
            this.nickName = nickName;
        }
        if (watchStyle != null) {
            this.watchStyle = watchStyle;
        }
        if (clubId != null) {
            this.clubId = clubId;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }

    public void updateAlarm(UserAlarmType alarmType, boolean isEnabled) {
        char status = isEnabled ? 'Y' : 'N';

        if (alarmType == UserAlarmType.ALL) {
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

    public boolean isAllAlarmEnabled() {
        return 'Y' == this.allAlarm;
    }

    public boolean isChatAlarmEnabled() {
        return 'Y' == this.chatAlarm;
    }

    public boolean isEnrollAlarmEnabled() {
        return 'Y' == this.enrollAlarm;
    }

    public boolean isEventAlarmEnabled() {
        return 'Y' == this.eventAlarm;
    }

    public void deleteFcmToken() {
        this.fcmToken = null;
    }

    public void markAsReported() {
        this.reported = true;
    }

}
