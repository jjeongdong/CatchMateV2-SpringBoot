package com.back.catchmate.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.auth.domain.exception.UnsupportedOAuthProviderException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProviderTest {

    @Test
    @DisplayName("대소문자를 무시하고 공급자를 찾는다")
    void findsProviderIgnoringCase() {
        // when & then
        assertThat(Provider.of("KaKaO")).isEqualTo(Provider.KAKAO);
        assertThat(Provider.of("google")).isEqualTo(Provider.GOOGLE);
    }

    @Test
    @DisplayName("지원하지 않는 공급자면 예외를 던진다")
    void throwsWhenUnsupported() {
        // when & then
        assertThatThrownBy(() -> Provider.of("naver")).isInstanceOf(UnsupportedOAuthProviderException.class);
    }

    @Test
    @DisplayName("공급자 값이 없으면 예외를 던진다")
    void throwsWhenNull() {
        // when & then
        assertThatThrownBy(() -> Provider.of(null)).isInstanceOf(UnsupportedOAuthProviderException.class);
    }
}
