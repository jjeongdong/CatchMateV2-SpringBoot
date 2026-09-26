package com.back.catchmate.auth.domain;

public interface OAuthClient {

    Provider supports();

    // 인가 code 로 공급자 토큰을 받고 사용자 정보를 조회한다. 실패하면 OAuthProviderException.
    OAuthProfile exchange(String code);

    String buildAuthorizeUrl(String state);
}
