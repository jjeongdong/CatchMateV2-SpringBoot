package com.back.catchmate.game.fixture;

import com.back.catchmate.game.domain.Game;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class GameFixture {

    private GameFixture() {}

    public static Game game(Long gameId, Long homeClubId, Long awayClubId, LocalDateTime gameStartDate) {
        Game game = Game.create(homeClubId, awayClubId, gameStartDate, "잠실");
        // 경기는 DB 시드로만 생성돼 id 를 세팅할 공개 경로가 없으므로 리플렉션으로 채운다.
        ReflectionTestUtils.setField(game, "id", gameId);
        return game;
    }
}
