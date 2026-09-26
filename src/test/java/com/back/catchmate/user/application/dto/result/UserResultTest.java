package com.back.catchmate.user.application.dto.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.user.fixture.UserFixture;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("JSON 키와 중첩 구단 키가 API 계약과 같다")
    void jsonKeys() throws Exception {
        // given
        UserResult result = UserResult.of(UserFixture.user(1L, 3L), new ClubInfo(3L, "LG 트윈스", "잠실", "서울"));

        // when
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(result));

        // then
        assertThat(json.fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder(
                        "userId",
                        "nickName",
                        "email",
                        "profileImageUrl",
                        "gender",
                        "birthDate",
                        "watchStyle",
                        "club",
                        "authority");
        assertThat(json.get("club").fieldNames())
                .toIterable()
                .containsExactlyInAnyOrder("clubId", "name", "homeStadium", "region");
    }

    @Test
    @DisplayName("구단 정보가 없으면 club 은 null 이다")
    void nullClub() {
        assertThat(UserResult.of(UserFixture.user(1L, 3L), null).club()).isNull();
    }
}
