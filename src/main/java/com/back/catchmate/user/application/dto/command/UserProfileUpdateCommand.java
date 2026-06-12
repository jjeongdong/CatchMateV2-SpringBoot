package com.back.catchmate.user.application.dto.command;


public record UserProfileUpdateCommand(
        String nickName,
        String watchStyle,
        Long favoriteClubId
) {
    public boolean hasFavoriteClubChange() {
        return favoriteClubId != null;
    }
}
