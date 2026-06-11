package com.back.catchmate.user.domain.enums;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
public enum Provider {
    KAKAO("kakao");

    @Getter
    private final String provider;

    public static Provider of(String provider) {
        return Stream.of(Provider.values())
                .filter(p -> p.getProvider().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new BaseException(ErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
