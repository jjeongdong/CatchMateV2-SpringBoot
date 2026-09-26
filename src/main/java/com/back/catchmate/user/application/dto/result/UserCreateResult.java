package com.back.catchmate.user.application.dto.result;

import com.back.catchmate.user.domain.User;
import java.time.LocalDateTime;

public record UserCreateResult(Long userId, String authority, LocalDateTime createdAt) {
    public static UserCreateResult from(User user) {
        return new UserCreateResult(user.getId(), user.getAuthority().name(), user.getCreatedAt());
    }
}
