package com.back.catchmate.user.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record UserNicknameCheckResponse(
        String nickName,

        @JsonProperty("available")
        boolean isAvailable
) {
    public static UserNicknameCheckResponse of(String nickName, boolean isAvailable) {
        return new UserNicknameCheckResponse(nickName, isAvailable);
    }
}
