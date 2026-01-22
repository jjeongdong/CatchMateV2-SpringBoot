package com.back.catchmate.domain.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum Provider {
    GOOGLE("google"),
    NAVER("naver"),
    KAKAO("kakao"),
    APPLE("apple");

    private final String provider;

    public static Provider of(String provider) {
        return Stream.of(Provider.values())
                .filter(p -> p.getProvider().equals(provider))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
