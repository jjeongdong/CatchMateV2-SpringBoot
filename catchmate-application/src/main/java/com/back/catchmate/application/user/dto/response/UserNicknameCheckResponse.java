package com.back.catchmate.application.user.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class UserNicknameCheckResponse {
    private final String nickName;
    private final boolean isAvailable;

    public static UserNicknameCheckResponse of(String nickName, boolean isAvailable) {
        return UserNicknameCheckResponse.builder()
                .nickName(nickName)
                .isAvailable(isAvailable)
                .build();
    }
}
