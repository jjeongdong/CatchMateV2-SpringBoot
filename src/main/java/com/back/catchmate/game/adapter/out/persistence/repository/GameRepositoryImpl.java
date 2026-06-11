package com.back.catchmate.game.adapter.out.persistence.repository;

import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.game.application.port.out.GameRepository;
import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import com.back.catchmate.game.domain.model.Game;
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
