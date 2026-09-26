package com.back.catchmate.global.config.security;

// 구독 목적지의 권한 검사는 그 목적지를 가진 BC 가 제공한다. global 은 목적지의 의미를 모른다.
public interface StompSubscriptionAuthorizer {

    boolean supports(String destination);

    // 권한이 없으면 BusinessException 을 던진다.
    void authorize(Long userId, String destination);
}
