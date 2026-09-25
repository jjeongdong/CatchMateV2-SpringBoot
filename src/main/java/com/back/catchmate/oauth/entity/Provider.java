package com.back.catchmate.oauth.entity;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * OAuth 인증 제공자. oauth 컨텍스트의 도메인 enum — user 컨텍스트는 String 으로만 저장한다.
 */
@RequiredArgsConstructor
public enum Provider {
    KAKAO("kakao"),
    GOOGLE("google");

    @Getter
    private final String provider;

    public static Provider of(String provider) {
        return Stream.of(Provider.values())
                .filter(p -> p.getProvider().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
