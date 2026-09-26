package com.back.catchmate.user.application.dto.api;

import com.back.catchmate.user.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserInfo(
        Long userId,
        String email,
        String provider,
        String providerId,
        Character gender,
        String nickName,
        LocalDate birthDate,
        String watchStyle,
        String profileImageUrl,
        String authority,
        String fcmToken,
        Long clubId,
        boolean chatAlarmEnabled,
        boolean enrollAlarmEnabled,
        boolean eventAlarmEnabled,
        boolean reported,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getProvider(),
                user.getProviderId(),
                user.getGender(),
                user.getNickName(),
                user.getBirthDate(),
                user.getWatchStyle(),
                user.getProfileImageUrl(),
                user.getAuthority().name(),
                user.getFcmToken(),
                user.getClubId(),
                user.isChatAlarmEnabled(),
                user.isEnrollAlarmEnabled(),
                user.isEventAlarmEnabled(),
                user.isReported(),
                user.getCreatedAt(),
                user.getModifiedAt());
    }
}
