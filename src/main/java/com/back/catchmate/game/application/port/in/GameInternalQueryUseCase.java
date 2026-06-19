package com.back.catchmate.game.application.port.in;

import com.back.catchmate.game.application.dto.response.GameInternalResponse;

import java.time.LocalDate;
import java.util.List;

public interface GameInternalQueryUseCase {
    GameInternalResponse getGame(Long gameId);

    List<GameInternalResponse> getGames(List<Long> gameIds);

    /**
     * 해당 날짜에 시작하는 경기 ID 목록.
     * 호출자(외부 컨텍스트)가 game.gameStartDate 컬럼에 의존하지 않도록 식별자만 반환.
     */
    List<Long> findIdsByGameStartDateOn(LocalDate gameDate);
}
