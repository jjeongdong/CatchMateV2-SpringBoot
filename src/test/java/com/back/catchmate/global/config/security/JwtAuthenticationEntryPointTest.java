package com.back.catchmate.global.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JwtAuthenticationEntryPointTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAuthenticationEntryPoint entryPoint = new JwtAuthenticationEntryPoint(objectMapper);

    @Test
    @DisplayName("인증 실패는 401 과 UNAUTHORIZED 코드만 담은 본문으로 응답한다")
    void respondsUnauthorized() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("토큰 없음"));

        // then
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("code").asText()).isEqualTo("UNAUTHORIZED");
        assertThat(body.get("message").asText()).isEqualTo("인증에 실패했습니다.");
        assertThat(body.size()).isEqualTo(2);
    }
}
