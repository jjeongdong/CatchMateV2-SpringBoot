package com.back.catchmate.game.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameResultTest {

    @Test
    @DisplayName("경기 목록 JSON 필드명이 기존 GameResponse·GameClubView 와 같다")
    void keepsJsonFieldNames() throws JsonProcessingException {
        // given
        GameResult result = new GameResult(
                1L,
                LocalDateTime.of(2026, 5, 1, 18, 30),
                "잠실",
                new GameResult.ClubView(10L, "LG 트윈스", "잠실", "서울"),
                null);
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));

        // then
        assertThat(json.properties())
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("gameId", "gameStartDate", "location", "homeClub", "awayClub");
        assertThat(json.get("homeClub").properties())
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("clubId", "name", "homeStadium", "region");
        assertThat(json.get("awayClub").isNull()).isTrue();
    }
}
