package com.back.catchmate.game.application.port.out.persistence;

import com.back.catchmate.game.domain.dto.GameSearchCondition;
import com.back.catchmate.game.domain.model.Game;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GameRepository {
    Game save(Game game);

    Optional<Game> findById(Long id);

    List<Game> findAllByIds(List<Long> ids);

    /**
     * 프론트 경기 선택 화면용 조회. 조건(날짜/구단)은 동적 필터링하며 시작 시각 오름차순으로 정렬한다.
     */
    List<Game> findAllByCondition(GameSearchCondition condition);

    /**
     * 해당 날짜(00:00 ~ 익일 00:00)에 시작하는 경기 ID 목록 반환.
     * board 등 외부 컨텍스트가 game.gameStartDate 컬럼을 직접 참조하지 않고 사전 조회로 사용.
     */
    List<Long> findIdsByGameStartDateOn(LocalDate gameDate);
}
