package com.back.catchmate.infrastructure.persistence.game.repository;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.game.repository.GameRepository;
import com.back.catchmate.infrastructure.persistence.game.entity.GameEntity;
import com.back.catchmate.domain.game.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {
    private final JpaGameRepository jpaGameRepository;

    @Override
    public Game save(Game game) {
        GameEntity entity = GameEntity.fromDomain(game);
        return jpaGameRepository.save(entity).toDomain();
    }

    @Override
    public Optional<Game> findByHomeClubAndAwayClubAndGameStartDate(Club homeClub, Club awayClub, LocalDateTime gameStartDate) {
        return jpaGameRepository.findByHomeClubIdAndAwayClubIdAndGameStartDate(
                homeClub.getId(),
                awayClub.getId(),
                gameStartDate
        ).map(GameEntity::toDomain);
    }
}
