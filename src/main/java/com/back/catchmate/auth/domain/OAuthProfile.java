package com.back.catchmate.auth.domain;

// 공급자에게서 받은 사용자 정보. 가입 전에는 signup token 에 담겨 클라이언트를 한 번 거쳐 돌아온다.
public record OAuthProfile(Provider provider, String providerId, String email, String profileImageUrl) {

    private static final String SEPARATOR = "@";

    // users.provider_id 에 저장된 형식이라 바꾸면 기존 회원을 찾지 못한다.
    public String providerIdWithProvider() {
        return providerId + SEPARATOR + provider.value();
    }
}
