package com.back.catchmate.game.repository;

import static com.back.catchmate.game.entity.QGame.game;

import com.back.catchmate.game.dto.request.GameSearchCondition;
import com.back.catchmate.game.entity.Game;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GameRepositoryImpl implements GameRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<Game> findAllByCondition(GameSearchCondition condition) {
        return jpaQueryFactory
                .selectFrom(game)
                .where(onDate(condition.gameDate()), involvesClub(condition.clubId()))
                .orderBy(game.gameStartDate.asc(), game.id.asc())
                .fetch();
    }

    private BooleanExpression onDate(LocalDate gameDate) {
        if (gameDate == null) {
            return null;
        }
        LocalDateTime start = gameDate.atStartOfDay();
        LocalDateTime end = gameDate.plusDays(1).atStartOfDay();
        return game.gameStartDate.goe(start).and(game.gameStartDate.lt(end));
    }

    private BooleanExpression involvesClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return game.homeClubId.eq(clubId).or(game.awayClubId.eq(clubId));
    }
}
