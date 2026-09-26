package com.back.catchmate.game.infrastructure;

import com.back.catchmate.game.domain.Game;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameJpaRepository extends JpaRepository<Game, Long> {
    // [start, end) 반개구간 조회. Spring Data 의 Between 은 양끝을 포함하므로 파생 쿼리로 대체하지 말 것.
    @Query("SELECT g.id FROM Game g WHERE g.gameStartDate >= :start AND g.gameStartDate < :end")
    List<Long> findIdsByGameStartDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
