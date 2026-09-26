package com.back.catchmate.game.infrastructure;

import static com.back.catchmate.game.domain.QGame.game;

import com.back.catchmate.game.domain.Game;
import com.back.catchmate.game.domain.GameRepository;
import com.back.catchmate.game.domain.GameSearchCondition;
import com.back.catchmate.game.domain.exception.GameNotFoundException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepository {
    private final GameJpaRepository gameJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Game getById(Long gameId) {
        return gameJpaRepository.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    @Override
    public List<Game> findAllByIds(Collection<Long> gameIds) {
        return gameJpaRepository.findAllById(gameIds);
    }

    @Override
    public List<Game> findAllByCondition(GameSearchCondition condition) {
        return jpaQueryFactory
                .selectFrom(game)
                .where(onDate(condition.gameDate()), involvesClub(condition.clubId()))
                .orderBy(game.gameStartDate.asc(), game.id.asc())
                .fetch();
    }

    @Override
    public List<Long> findIdsStartingOn(LocalDate gameDate) {
        return gameJpaRepository.findIdsByGameStartDateRange(
                gameDate.atStartOfDay(), gameDate.plusDays(1).atStartOfDay());
    }

    // QueryDSL 은 where 인자가 null 이면 그 조건을 건너뛰므로, 필터가 없을 때 null 을 돌려준다.
    private BooleanExpression onDate(LocalDate gameDate) {
        if (gameDate == null) {
            return null;
        }
        return game.gameStartDate
                .goe(gameDate.atStartOfDay())
                .and(game.gameStartDate.lt(gameDate.plusDays(1).atStartOfDay()));
    }

    private BooleanExpression involvesClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return game.homeClubId.eq(clubId).or(game.awayClubId.eq(clubId));
    }
}
