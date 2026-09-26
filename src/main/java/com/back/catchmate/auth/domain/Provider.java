package com.back.catchmate.auth.domain;

import com.back.catchmate.auth.domain.exception.UnsupportedOAuthProviderException;
import java.util.Arrays;

// user 컨텍스트는 이 값을 문자열(value)로만 저장한다.
public enum Provider {
    KAKAO("kakao"),
    GOOGLE("google");

    private final String value;

    Provider(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static Provider of(String value) {
        return Arrays.stream(values())
                .filter(provider -> provider.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(UnsupportedOAuthProviderException::new);
    }
}
