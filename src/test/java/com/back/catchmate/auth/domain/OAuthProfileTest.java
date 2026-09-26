package com.back.catchmate.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthProfileTest {

    @Test
    @DisplayName("users.provider_id 에 저장된 형식({id}@{공급자})으로 식별자를 만든다")
    void providerIdWithProvider() {
        // given
        OAuthProfile profile = new OAuthProfile(Provider.KAKAO, "12345", "a@b.com", null);

        // when & then
        assertThat(profile.providerIdWithProvider()).isEqualTo("12345@kakao");
    }
}
