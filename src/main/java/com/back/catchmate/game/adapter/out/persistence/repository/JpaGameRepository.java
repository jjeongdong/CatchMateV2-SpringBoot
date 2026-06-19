package com.back.catchmate.game.adapter.out.persistence.repository;

import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface JpaGameRepository extends JpaRepository<GameEntity, Long> {
    Optional<GameEntity> findByHomeClubIdAndAwayClubIdAndGameStartDate(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate);

    @Query("SELECT g.id FROM GameEntity g " +
            "WHERE g.gameStartDate >= :start AND g.gameStartDate < :end")
    List<Long> findIdsByGameStartDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
