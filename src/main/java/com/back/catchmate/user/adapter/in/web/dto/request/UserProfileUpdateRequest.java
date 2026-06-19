package com.back.catchmate.user.adapter.in.web.dto.request;

import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.") String nickName,
        Long favoriteClubId,
        String watchStyle
) {
    public UserProfileUpdateCommand toCommand() {
        return new UserProfileUpdateCommand(
                this.nickName,
                this.watchStyle,
                this.favoriteClubId
        );
    }
}
