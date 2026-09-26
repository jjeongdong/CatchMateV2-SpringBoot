package com.back.catchmate.game.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface GameRepository {
    Game getById(Long gameId);

    List<Game> findAllByIds(Collection<Long> gameIds);

    /** 날짜·구단 동적 필터. 시작 시각 오름차순, 같으면 id 오름차순. */
    List<Game> findAllByCondition(GameSearchCondition condition);

    /** gameDate 00:00 이상 다음날 00:00 미만에 시작하는 경기 ID. */
    List<Long> findIdsStartingOn(LocalDate gameDate);
}
