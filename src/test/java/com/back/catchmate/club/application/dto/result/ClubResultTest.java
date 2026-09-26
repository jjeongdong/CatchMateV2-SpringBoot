package com.back.catchmate.club.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubResultTest {

    @Test
    @DisplayName("구단 목록 JSON 필드명이 기존 ClubResponse 와 같다")
    void keepsJsonFieldNames() throws JsonProcessingException {
        // given
        ClubResult result = new ClubResult(1L, "LG 트윈스", "잠실", "서울");

        // when
        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(result));

        // then
        assertThat(json.properties())
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("clubId", "name", "homeStadium", "region");
    }
}
