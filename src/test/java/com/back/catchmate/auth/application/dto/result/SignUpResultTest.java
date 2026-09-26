package com.back.catchmate.auth.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SignUpResultTest {

    @Test
    @DisplayName("응답 JSON 에 refresh token 이 포함되지 않는다")
    void excludesRefreshTokenFromJson() throws JsonProcessingException {
        // given
        SignUpResult result = new SignUpResult(1L, "Bearer access", LocalDateTime.of(2026, 1, 1, 12, 0), "refresh");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // when
        String json = objectMapper.writeValueAsString(result);

        // then
        assertThat(json).contains("\"userId\":1", "\"accessToken\":\"Bearer access\"", "\"createdAt\"");
        assertThat(json).doesNotContain("refreshToken");
    }
}
