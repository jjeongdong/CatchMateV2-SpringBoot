package com.back.catchmate.game.repository;

import com.back.catchmate.game.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long>, GameRepositoryCustom {
    /**
     * [start, end) 반개구간 조회. Spring Data 의 Between 은 양끝을 포함하므로 파생 쿼리로 대체하지 말 것.
     */
    @Query("SELECT g.id FROM Game g " +
            "WHERE g.gameStartDate >= :start AND g.gameStartDate < :end")
    List<Long> findIdsByGameStartDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
