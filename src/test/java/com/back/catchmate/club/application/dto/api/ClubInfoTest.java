package com.back.catchmate.club.application.dto.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubInfoTest {

    @Test
    @DisplayName("타 BC 응답에 실리는 JSON 필드명이 기존 ClubSummary 와 같다")
    void keepsJsonFieldNames() throws JsonProcessingException {
        // given
        ClubInfo info = new ClubInfo(1L, "LG 트윈스", "잠실", "서울");

        // when
        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(info));

        // then
        assertThat(json.properties())
                .extracting(Map.Entry::getKey)
                .containsExactlyInAnyOrder("clubId", "name", "homeStadium", "region");
    }
}
