package com.back.catchmate.game.adapter.out.persistence.repository;

import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import com.back.catchmate.game.application.port.out.persistence.GameRepository;
import com.back.catchmate.game.domain.dto.GameSearchCondition;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {
    private final JpaGameRepository jpaGameRepository;
    private final QueryDslGameRepository queryDslGameRepository;

    @Override
    public Game save(Game game) {
        GameEntity entity = GameEntity.from(game);
        return jpaGameRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Game> findById(Long id) {
        return jpaGameRepository.findById(id).map(GameEntity::toDomain);
    }

    @Override
    public List<Game> findAllByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        return jpaGameRepository.findAllById(ids).stream()
                .map(GameEntity::toDomain)
                .toList();
    }

    @Override
    public List<Game> findAllByCondition(GameSearchCondition condition) {
        return queryDslGameRepository.findAllByCondition(condition).stream()
                .map(GameEntity::toDomain)
                .toList();
    }

    @Override
    public List<Long> findIdsByGameStartDateOn(LocalDate gameDate) {
        LocalDateTime start = gameDate.atStartOfDay();
        LocalDateTime end = gameDate.plusDays(1).atStartOfDay();
        return jpaGameRepository.findIdsByGameStartDateBetween(start, end);
    }
}
