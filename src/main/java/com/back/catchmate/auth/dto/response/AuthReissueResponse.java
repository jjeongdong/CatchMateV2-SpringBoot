package com.back.catchmate.auth.dto.response;

public record AuthReissueResponse(String accessToken) {
    public static AuthReissueResponse of(String accessToken) {
        return new AuthReissueResponse(accessToken);
    }
}
