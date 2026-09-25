package com.back.catchmate.user.dto.response;

import com.back.catchmate.user.entity.User;
import java.time.LocalDateTime;

public record CreatedUserResponse(Long userId, String authority, LocalDateTime createdAt) {
    public static CreatedUserResponse from(User user) {
        return new CreatedUserResponse(user.getId(), user.getAuthority().name(), user.getCreatedAt());
    }
}
