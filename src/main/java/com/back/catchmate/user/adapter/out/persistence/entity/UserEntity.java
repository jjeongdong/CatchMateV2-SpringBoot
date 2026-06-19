package com.back.catchmate.user.adapter.out.persistence.entity;

import com.back.catchmate.user.domain.model.Authority;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
public class UserEntity extends BaseTimeEntity {
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

    public static UserEntity from(User user) {
        return UserEntity.builder()
                .id(user.getId())
                .email(user.getEmail())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .gender(user.getGender())
                .nickName(user.getNickName())
                .birthDate(user.getBirthDate())
                .watchStyle(user.getWatchStyle())
                .profileImageUrl(user.getProfileImageUrl())
                .allAlarm(user.getAllAlarm())
                .chatAlarm(user.getChatAlarm())
                .enrollAlarm(user.getEnrollAlarm())
                .eventAlarm(user.getEventAlarm())
                .fcmToken(user.getFcmToken())
                .authority(user.getAuthority())
                .reported(user.isReported())
                .clubId(user.getClubId())
                .deletedAt(user.getDeletedAt())
                .build();
    }

    public User toDomain() {
        return User.builder()
                .id(this.id)
                .email(this.email)
                .provider(this.provider)
                .providerId(this.providerId)
                .gender(this.gender)
                .nickName(this.nickName)
                .birthDate(this.birthDate)
                .watchStyle(this.watchStyle)
                .profileImageUrl(this.profileImageUrl)
                .allAlarm(this.allAlarm)
                .chatAlarm(this.chatAlarm)
                .enrollAlarm(this.enrollAlarm)
                .eventAlarm(this.eventAlarm)
                .fcmToken(this.fcmToken)
                .authority(this.authority)
                .reported(this.reported)
                .clubId(this.clubId)
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getModifiedAt())
                .deletedAt(this.deletedAt)
                .build();
    }
}
