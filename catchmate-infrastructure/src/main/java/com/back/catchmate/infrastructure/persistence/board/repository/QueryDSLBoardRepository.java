package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.club.entity.QClubEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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

        // 1. 동적 쿼리 빌더 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 게시글만 조회
         builder.and(boardEntity.deletedAt.isNull());

        // 저장된 게시글만 조회
        builder.and(boardEntity.completed.isTrue());

        // 최대 인원수 필터
        if (condition.getMaxPerson() != null) {
            builder.and(boardEntity.maxPerson.eq(condition.getMaxPerson()));
        }

        // 응원팀 필터
        if (condition.getPreferredTeamIdList() != null && !condition.getPreferredTeamIdList().isEmpty()) {
            builder.and(boardEntity.cheerClub.id.in(condition.getPreferredTeamIdList()));
        }

        // 경기 날짜 필터
        if (condition.getGameDate() != null) {
            builder.and(boardEntity.game.gameStartDate.goe(condition.getGameDate().atStartOfDay()));
            builder.and(boardEntity.game.gameStartDate.lt(condition.getGameDate().plusDays(1).atStartOfDay()));
        }

        // 차단된 유저 필터 (UseCase에서 전달받은 ID 목록 사용)
        if (condition.getBlockedUserIds() != null && !condition.getBlockedUserIds().isEmpty()) {
            builder.and(boardEntity.user.id.notIn(condition.getBlockedUserIds()));
        }

        JPAQuery<Long> idQuery = jpaQueryFactory
                .select(boardEntity.id)
                .from(boardEntity);

        if (condition.getGameDate() != null) {
            idQuery.join(boardEntity.game, gameEntity);
        }

        int pageSize = pageable.getPageSize();

        List<Long> boardIds = idQuery
                .where(builder)
                .orderBy(boardEntity.liftUpDate.desc())
                .offset(pageable.getOffset())
                .limit(pageSize + 1) // 여기서 +1을 해줍니다!
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

        BooleanBuilder builder = new BooleanBuilder();

        // @SQLRestriction("deleted_at IS NULL")이 자동 적용되므로 중복 조건 불필요
        builder.and(boardEntity.completed.isTrue());

        if (condition.getMaxPerson() != null) {
            builder.and(boardEntity.maxPerson.eq(condition.getMaxPerson()));
        }

        if (condition.getPreferredTeamIdList() != null && !condition.getPreferredTeamIdList().isEmpty()) {
            builder.and(boardEntity.cheerClub.id.in(condition.getPreferredTeamIdList()));
        }

        if (condition.getGameDate() != null) {
            builder.and(boardEntity.game.gameStartDate.goe(condition.getGameDate().atStartOfDay()));
            builder.and(boardEntity.game.gameStartDate.lt(condition.getGameDate().plusDays(1).atStartOfDay()));
        }

        if (condition.getBlockedUserIds() != null && !condition.getBlockedUserIds().isEmpty()) {
            builder.and(boardEntity.user.id.notIn(condition.getBlockedUserIds()));
        }

        // 복합 커서 조건: liftUpDate < last OR (liftUpDate = last AND boardId < lastId)
        if (condition.getLastLiftUpDate() != null && condition.getLastBoardId() != null) {
            BooleanBuilder cursorCond = new BooleanBuilder();
            cursorCond.or(boardEntity.liftUpDate.lt(condition.getLastLiftUpDate()));
            cursorCond.or(
                    boardEntity.liftUpDate.eq(condition.getLastLiftUpDate())
                            .and(boardEntity.id.lt(condition.getLastBoardId()))
            );
            builder.and(cursorCond);
        }

        JPAQuery<Long> idQuery = jpaQueryFactory
                .select(boardEntity.id)
                .from(boardEntity);

        if (condition.getGameDate() != null) {
            idQuery.join(boardEntity.game, gameEntity);
        }

        List<Long> boardIds = idQuery
                .where(builder)
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
}
