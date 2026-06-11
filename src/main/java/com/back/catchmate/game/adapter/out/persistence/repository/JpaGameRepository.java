package com.back.catchmate.game.adapter.out.persistence.repository;

import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface JpaGameRepository extends JpaRepository<GameEntity, Long> {
    Optional<GameEntity> findByHomeClubIdAndAwayClubIdAndGameStartDate(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate);
}
