package com.back.catchmate.domain.user.model;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.common.permission.ResourceOwnership;
import lombok.Builder;
import lombok.Getter;
import com.back.catchmate.user.enums.AlarmType;
import com.back.catchmate.user.enums.Provider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Builder
public class User implements ResourceOwnership {
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
    private boolean reported;
    private Club club;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static User createUser(Provider provider, String providerId, String email, String nickName, Character gender,
                                  LocalDate birthDate, Club favoriteClub, String profileImageUrl, String fcmToken,
                                  String watchStyle) {
        return User.builder()
                .email(email)
                .provider(provider)
                .providerId(providerId)
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

    public void markAsReported() {
        this.reported = true;
    }

    @Override
    public Long getOwnershipId() {
        return this.id;
    }
}
