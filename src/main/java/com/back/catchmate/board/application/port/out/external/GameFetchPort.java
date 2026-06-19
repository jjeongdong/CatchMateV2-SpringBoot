package com.back.catchmate.board.application.port.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;

import java.time.LocalDate;
import java.util.List;

public interface GameFetchPort {
    BoardGameInfo getGame(Long gameId);

    List<BoardGameInfo> getGames(List<Long> gameIds);

    /**
     * 해당 날짜의 경기 ID 목록 — game 컨텍스트의 gameStartDate 컬럼을 직접 참조하지 않기 위한 사전 조회.
     */
    List<Long> findGameIdsByDate(LocalDate gameDate);
}
