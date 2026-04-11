package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.club.entity.QClubEntity;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.back.catchmate.infrastructure.persistence.board.entity.QBoardEntity.boardEntity;
import static com.back.catchmate.infrastructure.persistence.game.entity.QGameEntity.gameEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class QueryDSLBoardRepository {
    private final JPAQueryFactory jpaQueryFactory;

    public Page<BoardEntity> findAllByCondition(BoardSearchCondition condition, Pageable pageable) {
        QClubEntity cheerClub = new QClubEntity("cheerClub");
        QClubEntity homeClub  = new QClubEntity("homeClub");
        QClubEntity awayClub  = new QClubEntity("awayClub");

        JPAQuery<Long> idQuery = jpaQueryFactory
                .select(boardEntity.id)
                .from(boardEntity);

        if (condition.getGameDate() != null) {
            idQuery.join(boardEntity.game, gameEntity);
        }

        int pageSize = pageable.getPageSize();

        List<Long> boardIds = idQuery
                .where(
                        boardEntity.completed.isTrue(),
                        eqMaxPerson(condition.getMaxPerson()),
                        inPreferredTeams(condition.getPreferredTeamIdList()),
                        onGameDate(condition.getGameDate()),
                        notInBlockedUsers(condition.getBlockedUserIds())
                )
                .orderBy(boardEntity.liftUpDate.desc())
                .offset(pageable.getOffset())
                .limit(pageSize + 1)
                .fetch();

        boolean hasNext = false;
        if (boardIds.size() > pageSize) {
            boardIds.remove(pageSize);
            hasNext = true;
        }

        if (boardIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<BoardEntity> content = jpaQueryFactory
                .selectFrom(boardEntity)
                .leftJoin(boardEntity.game, gameEntity).fetchJoin()
                .leftJoin(boardEntity.cheerClub, cheerClub).fetchJoin()
                .leftJoin(boardEntity.game.homeClub, homeClub).fetchJoin()
                .leftJoin(boardEntity.game.awayClub, awayClub).fetchJoin()
                .leftJoin(boardEntity.user, userEntity).fetchJoin()
                .where(boardEntity.id.in(boardIds))
                .orderBy(boardEntity.liftUpDate.desc())
                .fetch();

        long totalElements = hasNext ? pageable.getOffset() + pageSize + 1 : pageable.getOffset() + content.size();

        return new PageImpl<>(content, pageable, totalElements);
    }

    /**
     * No-Offset 커서 기반 조회 — offset() 없이 WHERE 조건으로 다음 페이지를 결정합니다.
     * fetchSize = size + 1 로 호출하면 hasNext 판단을 호출자(BoardRepositoryImpl)에서 처리할 수 있습니다.
     */
    public List<BoardEntity> findAllByConditionWithCursor(BoardSearchCondition condition, int fetchSize) {
        QClubEntity cheerClub = new QClubEntity("cheerClub");
        QClubEntity homeClub  = new QClubEntity("homeClub");
        QClubEntity awayClub  = new QClubEntity("awayClub");

        JPAQuery<Long> idQuery = jpaQueryFactory
                .select(boardEntity.id)
                .from(boardEntity);

        if (condition.getGameDate() != null) {
            idQuery.join(boardEntity.game, gameEntity);
        }

        List<Long> boardIds = idQuery
                .where(
                        boardEntity.completed.isTrue(),
                        eqMaxPerson(condition.getMaxPerson()),
                        inPreferredTeams(condition.getPreferredTeamIdList()),
                        onGameDate(condition.getGameDate()),
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
                .leftJoin(boardEntity.game, gameEntity).fetchJoin()
                .leftJoin(boardEntity.cheerClub, cheerClub).fetchJoin()
                .leftJoin(boardEntity.game.homeClub, homeClub).fetchJoin()
                .leftJoin(boardEntity.game.awayClub, awayClub).fetchJoin()
                .leftJoin(boardEntity.user, userEntity).fetchJoin()
                .where(boardEntity.id.in(boardIds))
                .orderBy(boardEntity.liftUpDate.desc(), boardEntity.id.desc())
                .fetch();
    }

    private BooleanExpression eqMaxPerson(Integer maxPerson) {
        return maxPerson != null ? boardEntity.maxPerson.eq(maxPerson) : null;
    }

    private BooleanExpression inPreferredTeams(List<Long> teamIds) {
        return teamIds != null && !teamIds.isEmpty() ? boardEntity.cheerClub.id.in(teamIds) : null;
    }

    private BooleanExpression onGameDate(LocalDate gameDate) {
        return gameDate != null
                ? boardEntity.game.gameStartDate.goe(gameDate.atStartOfDay())
                        .and(boardEntity.game.gameStartDate.lt(gameDate.plusDays(1).atStartOfDay()))
                : null;
    }

    private BooleanExpression notInBlockedUsers(List<Long> blockedUserIds) {
        return blockedUserIds != null && !blockedUserIds.isEmpty()
                ? boardEntity.user.id.notIn(blockedUserIds)
                : null;
    }

    private BooleanExpression cursorCondition(LocalDateTime lastLiftUpDate, Long lastBoardId) {
        if (lastLiftUpDate == null || lastBoardId == null) return null;
        return boardEntity.liftUpDate.lt(lastLiftUpDate)
                .or(boardEntity.liftUpDate.eq(lastLiftUpDate)
                        .and(boardEntity.id.lt(lastBoardId)));
    }
}
