package com.back.catchmate.user.domain;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseTimeEntity {
    // 알림 컬럼이 CHAR('Y'/'N') 로 저장돼 있어 스키마를 바꾸지 않고 그 표현을 유지한다.
    private static final char ALARM_ON = 'Y';
    private static final char ALARM_OFF = 'N';

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

    private User(
            String provider,
            String providerId,
            String email,
            String nickName,
            Character gender,
            LocalDate birthDate,
            Long clubId,
            String profileImageUrl,
            String watchStyle) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickName = nickName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.clubId = clubId;
        this.profileImageUrl = profileImageUrl;
        this.watchStyle = watchStyle;
        this.allAlarm = ALARM_ON;
        this.chatAlarm = ALARM_ON;
        this.enrollAlarm = ALARM_ON;
        this.eventAlarm = ALARM_ON;
        this.authority = Authority.ROLE_USER;
        this.reported = false;
    }

    public static User create(
            String provider,
            String providerId,
            String email,
            String nickName,
            Character gender,
            LocalDate birthDate,
            Long clubId,
            String profileImageUrl,
            String watchStyle) {
        return new User(provider, providerId, email, nickName, gender, birthDate, clubId, profileImageUrl, watchStyle);
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

    public void updateAlarm(UserAlarmType alarmType, boolean enabled) {
        char status = enabled ? ALARM_ON : ALARM_OFF;
        switch (alarmType) {
            case ALL -> {
                this.allAlarm = status;
                this.chatAlarm = status;
                this.enrollAlarm = status;
                this.eventAlarm = status;
            }
            case CHAT -> this.chatAlarm = status;
            case ENROLL -> this.enrollAlarm = status;
            case EVENT -> this.eventAlarm = status;
        }
    }

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void clearFcmToken() {
        this.fcmToken = null;
    }

    public void markAsReported() {
        this.reported = true;
    }

    public boolean isAllAlarmEnabled() {
        return ALARM_ON == this.allAlarm;
    }

    public boolean isChatAlarmEnabled() {
        return ALARM_ON == this.chatAlarm;
    }

    public boolean isEnrollAlarmEnabled() {
        return ALARM_ON == this.enrollAlarm;
    }

    public boolean isEventAlarmEnabled() {
        return ALARM_ON == this.eventAlarm;
    }
}
