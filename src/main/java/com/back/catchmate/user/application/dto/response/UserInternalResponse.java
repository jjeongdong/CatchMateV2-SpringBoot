package com.back.catchmate.user.application.dto.response;

import com.back.catchmate.user.domain.model.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserInternalResponse(
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
        boolean allAlarmEnabled,
        boolean chatAlarmEnabled,
        boolean enrollAlarmEnabled,
        boolean eventAlarmEnabled,
        boolean reported,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserInternalResponse from(User user) {
        return new UserInternalResponse(
                user.getId(),
                user.getEmail(),
                user.getProvider(),
                user.getProviderId(),
                user.getGender(),
                user.getNickName(),
                user.getBirthDate(),
                user.getWatchStyle(),
                user.getProfileImageUrl(),
                user.getAuthority() != null ? user.getAuthority().name() : null,
                user.getFcmToken(),
                user.getClubId(),
                user.isAllAlarmEnabled(),
                user.isChatAlarmEnabled(),
                user.isEnrollAlarmEnabled(),
                user.isEventAlarmEnabled(),
                user.isReported(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
