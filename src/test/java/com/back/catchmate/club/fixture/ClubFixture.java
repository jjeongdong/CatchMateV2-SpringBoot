package com.back.catchmate.club.fixture;

import com.back.catchmate.club.domain.Club;
import org.springframework.test.util.ReflectionTestUtils;

public final class ClubFixture {

    private ClubFixture() {}

    public static Club lgTwins(Long clubId) {
        return withId(Club.create("LG 트윈스", "잠실", "서울"), clubId);
    }

    public static Club doosanBears(Long clubId) {
        return withId(Club.create("두산 베어스", "잠실", "서울"), clubId);
    }

    // 구단은 DB 시드로만 생성돼 id 를 세팅할 공개 경로가 없으므로 리플렉션으로 채운다.
    private static Club withId(Club club, Long clubId) {
        ReflectionTestUtils.setField(club, "id", clubId);
        return club;
    }
}
