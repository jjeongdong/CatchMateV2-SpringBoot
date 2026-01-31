package user.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.stream.Stream;

@RequiredArgsConstructor
public enum Provider {
    GOOGLE("google"),
    NAVER("naver"),
    KAKAO("kakao"),
    APPLE("apple");

    @Getter
    private final String provider;

    public static Provider of(String provider) {
        return Stream.of(Provider.values())
                .filter(p -> p.getProvider().equals(provider))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
