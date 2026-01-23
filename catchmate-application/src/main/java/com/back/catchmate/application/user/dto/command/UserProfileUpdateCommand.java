package com.back.catchmate.application.user.dto.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserProfileUpdateCommand {
    private String nickName;
    private Long favoriteClubId;
    private String watchStyle;

    public boolean hasFavoriteClubChange() {
        return favoriteClubId != null;
    }
}
