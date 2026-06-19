package com.back.catchmate.auth.application.dto.response;

/**
 * 발급된 access/refresh 토큰 페어. auth 컨텍스트가 자체 소유한 published contract —
 * 외부(oauth 등) 호출자가 auth.domain.model.AuthToken 을 직접 import 하지 않도록 한다.
 */
public record IssuedAuthToken(
        String accessToken,
        String refreshToken
) {
}
