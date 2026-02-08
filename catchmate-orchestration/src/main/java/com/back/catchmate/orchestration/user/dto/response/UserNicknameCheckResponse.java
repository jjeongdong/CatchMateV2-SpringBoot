package com.back.catchmate.orchestration.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserNicknameCheckResponse {
    private String nickName;
    private boolean isAvailable;

    public static UserNicknameCheckResponse of(String nickName, boolean isAvailable) {
        return UserNicknameCheckResponse.builder()
                .nickName(nickName)
                .isAvailable(isAvailable)
                .build();
    }
}
