package com.back.catchmate.board.repository;

import static com.back.catchmate.board.entity.QBoard.board;

import com.back.catchmate.board.dto.request.BoardSearchCondition;
import com.back.catchmate.board.entity.Board;
import com.back.catchmate.common.response.CursorPage;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepositoryCustom {
    private final JPAQueryFactory jpaQueryFactory;

    /**
     * No-Offset 커서 기반 조회 — offset() 없이 WHERE 조건으로 다음 페이지를 결정합니다.
     * fetchSize = size + 1 로 호출하면 hasNext 판단을 호출자(BoardRepositoryImpl)에서 처리할 수 있습니다.
     */
    private List<Board> fetchByConditionWithCursor(BoardSearchCondition condition, int fetchSize) {
        List<Long> boardIds = jpaQueryFactory
                .select(board.id)
                .from(board)
                .where(
                        board.completed.isTrue(),
                        eqMaxPerson(condition.getMaxPerson()),
                        inPreferredTeams(condition.getPreferredTeamIdList()),
                        inMatchingGames(condition.getMatchingGameIds()),
                        notInBlockedUsers(condition.getBlockedUserIds()),
                        cursorCondition(condition.getLastLiftUpDate(), condition.getLastBoardId()))
                .orderBy(board.liftUpDate.desc(), board.id.desc())
                .limit(fetchSize)
                .fetch();

        if (boardIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jpaQueryFactory
                .selectFrom(board)
                .where(board.id.in(boardIds))
                .orderBy(board.liftUpDate.desc(), board.id.desc())
                .fetch();
    }

    private BooleanExpression eqMaxPerson(Integer maxPerson) {
        return maxPerson != null ? board.maxPerson.eq(maxPerson) : null;
    }

    private BooleanExpression inPreferredTeams(List<Long> teamIds) {
        return teamIds != null && !teamIds.isEmpty() ? board.cheerClubId.in(teamIds) : null;
    }

    private BooleanExpression inMatchingGames(List<Long> gameIds) {
        return gameIds != null && !gameIds.isEmpty() ? board.gameId.in(gameIds) : null;
    }

    private BooleanExpression notInBlockedUsers(List<Long> blockedUserIds) {
        return blockedUserIds != null && !blockedUserIds.isEmpty() ? board.userId.notIn(blockedUserIds) : null;
    }

    private BooleanExpression cursorCondition(LocalDateTime lastLiftUpDate, Long lastBoardId) {
        if (lastLiftUpDate == null || lastBoardId == null) return null;
        return board.liftUpDate
                .lt(lastLiftUpDate)
                .or(board.liftUpDate.eq(lastLiftUpDate).and(board.id.lt(lastBoardId)));
    }

    @Override
    public CursorPage<Board> findAllByConditionWithCursor(BoardSearchCondition condition, int size) {
        List<Board> entities = fetchByConditionWithCursor(condition, size + 1);

        boolean hasNext = entities.size() > size;
        if (hasNext) {
            entities = new ArrayList<>(entities);
            entities.remove(size);
        }

        Long nextCursorId = null;
        LocalDateTime nextCursorDateTime = null;
        if (hasNext && !entities.isEmpty()) {
            Board last = entities.get(entities.size() - 1);
            nextCursorId = last.getId();
            nextCursorDateTime = last.getLiftUpDate();
        }

        return new CursorPage<>(entities, hasNext, nextCursorId, nextCursorDateTime);
    }
}
