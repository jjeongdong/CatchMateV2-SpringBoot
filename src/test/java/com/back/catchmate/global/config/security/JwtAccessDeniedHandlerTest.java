package com.back.catchmate.global.config.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JwtAccessDeniedHandlerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final JwtAccessDeniedHandler handler = new JwtAccessDeniedHandler(objectMapper);

    @Test
    @DisplayName("필터 단계 권한 거부는 403 과 FORBIDDEN 코드만 담은 본문으로 응답한다")
    void respondsForbidden() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("관리자 전용"));

        // then
        JsonNode body = objectMapper.readTree(response.getContentAsString());
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(body.get("code").asText()).isEqualTo("FORBIDDEN");
        assertThat(body.get("message").asText()).isEqualTo("접근 권한이 없습니다.");
        assertThat(body.size()).isEqualTo(2);
    }
}
