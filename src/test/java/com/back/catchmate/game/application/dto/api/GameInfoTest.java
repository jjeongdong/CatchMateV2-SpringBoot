package com.back.catchmate.game.application.dto.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameInfoTest {

    @Test
    @DisplayName("타 BC 응답에 실리는 JSON 필드명이 기존 GameSummary 와 같다")
    void keepsJsonFieldNames() throws JsonProcessingException {
        // given
        GameInfo info = new GameInfo(1L, LocalDateTime.of(2026, 5, 1, 18, 30), "잠실", 10L, 20L);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(info));

        // then
        assertThat(json.properties())
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("gameId", "gameStartDate", "location", "homeClubId", "awayClubId");
    }
}
