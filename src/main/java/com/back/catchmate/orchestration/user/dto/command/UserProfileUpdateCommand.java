package com.back.catchmate.orchestration.user.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileUpdateCommand {
    private String nickName;
    private String watchStyle;
    private Long favoriteClubId;

    public boolean hasFavoriteClubChange() {
        return favoriteClubId != null;
    }
}
