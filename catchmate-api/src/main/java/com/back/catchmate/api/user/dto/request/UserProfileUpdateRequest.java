package com.back.catchmate.api.user.dto.request;

import com.back.catchmate.orchestration.user.dto.command.UserProfileUpdateCommand;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {
    @Size(min = 2, max = 10, message = "닉네임은 2~10자여야 합니다.")
    private String nickName;

    private Long favoriteClubId;

    private String watchStyle;

    public static UserProfileUpdateCommand toCommand(UserProfileUpdateRequest request) {
        if (request == null) {
            return com.back.catchmate.orchestration.user.dto.command.UserProfileUpdateCommand.builder().build();
        }
        return request.toCommand();
    }

    private UserProfileUpdateCommand toCommand() {
        return UserProfileUpdateCommand.builder()
                .nickName(nickName)
                .favoriteClubId(favoriteClubId)
                .watchStyle(watchStyle)
                .build();
    }
}
