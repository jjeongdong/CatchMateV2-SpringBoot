package com.back.catchmate.game.repository;

import com.back.catchmate.game.dto.request.GameSearchCondition;
import com.back.catchmate.game.entity.Game;

import java.util.List;

public interface GameRepositoryCustom {
    /**
     * 프론트 경기 선택 화면용 조회. 조건(날짜/구단)은 동적 필터링하며
     * 시작 시각 오름차순, 동시각이면 id 오름차순으로 정렬한다.
     */
    List<Game> findAllByCondition(GameSearchCondition condition);
}
