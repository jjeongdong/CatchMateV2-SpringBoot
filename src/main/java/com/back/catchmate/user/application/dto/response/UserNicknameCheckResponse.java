package com.back.catchmate.user.application.dto.response;


public record UserNicknameCheckResponse(
        String nickName,
        boolean isAvailable
) {
    public static UserNicknameCheckResponse of(String nickName, boolean isAvailable) {
        return new UserNicknameCheckResponse(nickName, isAvailable);
    }
}
