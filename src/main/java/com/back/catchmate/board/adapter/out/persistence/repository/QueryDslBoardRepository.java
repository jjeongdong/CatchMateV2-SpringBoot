package com.back.catchmate.board.adapter.out.persistence.repository;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.adapter.out.persistence.entity.BoardEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.back.catchmate.board.adapter.out.persistence.entity.QBoardEntity.boardEntity;

@Repository
@RequiredArgsConstructor
public class QueryDslBoardRepository {
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * No-Offset 커서 기반 조회 — offset() 없이 WHERE 조건으로 다음 페이지를 결정합니다.
     * fetchSize = size + 1 로 호출하면 hasNext 판단을 호출자(BoardRepositoryImpl)에서 처리할 수 있습니다.
     */
    public List<BoardEntity> findAllByConditionWithCursor(BoardSearchCondition condition, int fetchSize) {
        List<Long> boardIds = jpaQueryFactory
                .select(boardEntity.id)
                .from(boardEntity)
                .where(
                        boardEntity.completed.isTrue(),
                        eqMaxPerson(condition.getMaxPerson()),
                        inPreferredTeams(condition.getPreferredTeamIdList()),
                        inMatchingGames(condition.getMatchingGameIds()),
                        notInBlockedUsers(condition.getBlockedUserIds()),
                        cursorCondition(condition.getLastLiftUpDate(), condition.getLastBoardId())
                )
                .orderBy(boardEntity.liftUpDate.desc(), boardEntity.id.desc())
                .limit(fetchSize)
                .fetch();

        if (boardIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(boardEntity)
                .where(boardEntity.id.in(boardIds))
                .orderBy(boardEntity.liftUpDate.desc(), boardEntity.id.desc())
                .fetch();
    }

    private BooleanExpression eqMaxPerson(Integer maxPerson) {
        return maxPerson != null ? boardEntity.maxPerson.eq(maxPerson) : null;
    }

    private BooleanExpression inPreferredTeams(List<Long> teamIds) {
        return teamIds != null && !teamIds.isEmpty() ? boardEntity.cheerClubId.in(teamIds) : null;
    }

    private BooleanExpression inMatchingGames(List<Long> gameIds) {
        return gameIds != null && !gameIds.isEmpty() ? boardEntity.gameId.in(gameIds) : null;
    }

    private BooleanExpression notInBlockedUsers(List<Long> blockedUserIds) {
        return blockedUserIds != null && !blockedUserIds.isEmpty()
                ? boardEntity.userId.notIn(blockedUserIds)
                : null;
    }

    private BooleanExpression cursorCondition(LocalDateTime lastLiftUpDate, Long lastBoardId) {
        if (lastLiftUpDate == null || lastBoardId == null) return null;
        return boardEntity.liftUpDate.lt(lastLiftUpDate)
                .or(boardEntity.liftUpDate.eq(lastLiftUpDate)
                        .and(boardEntity.id.lt(lastBoardId)));
    }
}
