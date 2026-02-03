package com.back.catchmate.infrastructure.persistence.board.repository;

import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.club.entity.QClubEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.back.catchmate.infrastructure.persistence.board.entity.QBoardEntity.boardEntity;
import static com.back.catchmate.infrastructure.persistence.game.entity.QGameEntity.gameEntity;
import static com.back.catchmate.infrastructure.persistence.user.entity.QUserEntity.userEntity;

@Repository
@RequiredArgsConstructor
public class QueryDSLBoardRepositoryImpl implements QueryDSLBoardRepository {
    private final JPAQueryFactory queryFactory;

    @Override
    public Page<BoardEntity> findAllByCondition(BoardSearchCondition condition, Pageable pageable) {
        QClubEntity cheerClub = new QClubEntity("cheerClub");
        QClubEntity homeClub  = new QClubEntity("homeClub");
        QClubEntity awayClub  = new QClubEntity("awayClub");

        // QClass 정의 (ChatRoom은 파일에 없어 주석 처리했으나, 필요시 해제하여 사용)
        // QChatRoomEntity chatRoomEntity = QChatRoomEntity.chatRoomEntity;

        // 1. 동적 쿼리 빌더 생성
        BooleanBuilder builder = new BooleanBuilder();

        // 삭제되지 않은 게시글만 조회
//         builder.and(boardEntity.deletedAt.isNull());

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

        // 2. 쿼리 실행
        List<BoardEntity> content = queryFactory
                .selectFrom(boardEntity)
                .leftJoin(boardEntity.game, gameEntity).fetchJoin()
                .leftJoin(boardEntity.cheerClub, cheerClub).fetchJoin()
                .leftJoin(boardEntity.game.homeClub, homeClub).fetchJoin()
                .leftJoin(boardEntity.game.awayClub, awayClub).fetchJoin()
                .leftJoin(boardEntity.user, userEntity).fetchJoin()
                // .leftJoin(boardEntity.chatRoom, chatRoomEntity).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(boardEntity.liftUpDate.desc()) // 끌어올리기 최신순
                .fetch();

        // 3. Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(boardEntity.count())
                .from(boardEntity)
                .where(builder);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
