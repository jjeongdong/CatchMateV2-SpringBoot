package com.back.catchmate.infrastructure.persistence.game.repository;

import com.back.catchmate.infrastructure.persistence.game.entity.GameEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaGameRepository extends JpaRepository<GameEntity, Long> {
}
