package com.back.catchmate.user.dto.request;

import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.") String nickName,
        Long favoriteClubId,
        String watchStyle
) {
    public boolean hasFavoriteClubChange() {
        return favoriteClubId != null;
    }
}
