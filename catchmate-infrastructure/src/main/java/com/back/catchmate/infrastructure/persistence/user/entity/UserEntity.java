package com.back.catchmate.infrastructure.persistence.user.entity;

import com.back.catchmate.domain.user.model.Authority;
import com.back.catchmate.domain.user.model.Provider;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.club.entity.ClubEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@Builder
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private ClubEntity club;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

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
    private boolean isReported;

    public User toModel() {
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
                .isReported(this.isReported)
                .club(this.club.toModel())
                .createdAt(this.getCreatedAt())
                .updatedAt(this.getModifiedAt())
                .build();
    }

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
                .isReported(user.isReported())
                .club(ClubEntity.from(user.getClub()))
                .build();
    }
}
