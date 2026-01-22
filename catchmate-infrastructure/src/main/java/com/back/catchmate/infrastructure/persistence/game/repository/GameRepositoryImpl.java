package com.back.catchmate.infrastructure.persistence.game.repository;

import com.back.catchmate.domain.game.repository.GameRepository;
import com.back.catchmate.infrastructure.persistence.game.entity.GameEntity;
import com.back.catchmate.domain.game.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {
    private final JpaGameRepository jpaGameRepository;

    @Override
    public Game save(Game game) {
        GameEntity entity = GameEntity.from(game);
        return jpaGameRepository.save(entity).toModel();
    }

    @Override
    public Optional<Game> findById(Long id) {
        return jpaGameRepository.findById(id)
                .map(GameEntity::toModel);
    }
}
