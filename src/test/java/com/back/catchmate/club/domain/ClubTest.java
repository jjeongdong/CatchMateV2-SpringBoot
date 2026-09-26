package com.back.catchmate.club.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClubTest {

    @Test
    @DisplayName("구단명·홈구장·연고지로 구단을 만든다")
    void createsClub() {
        // when
        Club club = Club.create("LG 트윈스", "잠실", "서울");

        // then
        assertThat(club.getId()).isNull();
        assertThat(club.getName()).isEqualTo("LG 트윈스");
        assertThat(club.getHomeStadium()).isEqualTo("잠실");
        assertThat(club.getRegion()).isEqualTo("서울");
    }
}
