package com.back.catchmate.game.adapter.out.persistence.repository;

import com.back.catchmate.game.adapter.out.persistence.entity.GameEntity;
import com.back.catchmate.game.domain.dto.GameSearchCondition;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.back.catchmate.game.adapter.out.persistence.entity.QGameEntity.gameEntity;

@Repository
@RequiredArgsConstructor
public class QueryDslGameRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public List<GameEntity> findAllByCondition(GameSearchCondition condition) {
        return jpaQueryFactory
                .selectFrom(gameEntity)
                .where(
                        onDate(condition.gameDate()),
                        involvesClub(condition.clubId())
                )
                .orderBy(gameEntity.gameStartDate.asc(), gameEntity.id.asc())
                .fetch();
    }

    private BooleanExpression onDate(LocalDate gameDate) {
        if (gameDate == null) {
            return null;
        }
        LocalDateTime start = gameDate.atStartOfDay();
        LocalDateTime end = gameDate.plusDays(1).atStartOfDay();
        return gameEntity.gameStartDate.goe(start).and(gameEntity.gameStartDate.lt(end));
    }

    private BooleanExpression involvesClub(Long clubId) {
        if (clubId == null) {
            return null;
        }
        return gameEntity.homeClubId.eq(clubId).or(gameEntity.awayClubId.eq(clubId));
    }
}
