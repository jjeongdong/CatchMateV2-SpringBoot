package com.back.catchmate.application.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserNicknameCheckResponse {
    private String nickName;
    private boolean available;

    public static UserNicknameCheckResponse of(String nickName, boolean available) {
        return UserNicknameCheckResponse.builder()
                .nickName(nickName)
                .available(available)
                .build();
    }
}
