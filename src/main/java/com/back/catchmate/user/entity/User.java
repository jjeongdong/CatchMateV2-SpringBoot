package com.back.catchmate.user.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(nullable = false)
    private String email;

    /** OAuth 인증 제공자 식별자 — oauth.domain.enums.Provider 의 문자열 값(KAKAO 등)을 그대로 저장. */
    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String providerId;

    @Column(nullable = false)
    private Character gender;

    @Column(nullable = false)
    private String nickName;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column
    private String watchStyle;

    @Column(nullable = false)
    private String profileImageUrl;

    @Column(nullable = false)
    private Character allAlarm;

    @Column(nullable = false)
    private Character chatAlarm;

    @Column(nullable = false)
    private Character enrollAlarm;

    @Column(nullable = false)
    private Character eventAlarm;

    @Column
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Authority authority;

    @Column(nullable = false)
    private boolean reported;

    @Column
    private LocalDateTime deletedAt;

    public static User createUser(
            String provider,
            String providerId,
            String email,
            String nickName,
            Character gender,
            LocalDate birthDate,
            Long favoriteClubId,
            String profileImageUrl,
            String fcmToken,
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
