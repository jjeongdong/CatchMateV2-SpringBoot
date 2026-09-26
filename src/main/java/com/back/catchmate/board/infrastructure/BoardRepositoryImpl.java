package com.back.catchmate.board.infrastructure;

import static com.back.catchmate.board.domain.QBoard.board;

import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.BoardSearchCondition;
import com.back.catchmate.board.domain.exception.BoardNotFoundException;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BoardRepositoryImpl implements BoardRepository {
    private final BoardJpaRepository boardJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Board save(Board newBoard) {
        return boardJpaRepository.save(newBoard);
    }

    @Override
    public Board getById(Long boardId) {
        return boardJpaRepository.findById(boardId).orElseThrow(BoardNotFoundException::new);
    }

    @Override
    public Board getPublishedById(Long boardId) {
        return boardJpaRepository.findByIdAndCompletedTrue(boardId).orElseThrow(BoardNotFoundException::new);
    }

    @Override
    public List<Board> findAllByIds(Collection<Long> boardIds) {
        return boardJpaRepository.findAllById(boardIds);
    }

    @Override
    public Optional<Board> findDraftByWriterId(Long writerId) {
        return boardJpaRepository.findFirstByUserIdAndCompletedFalse(writerId);
    }

    @Override
    public void deleteDraft(Board draft) {
        // draft(미완성)는 일회성·고빈도라 물리 삭제 (soft-delete 누적 방지). 의도된 예외.
        boardJpaRepository.delete(draft); // arch-audit:allow-hard-delete
    }

    @Override
    public List<Board> findAllByCondition(BoardSearchCondition condition, int limit) {
        // 조건·정렬로 ID 만 먼저 고르고 본문은 IN 으로 읽는다 (옛 조회 방식 유지).
        List<Long> boardIds = jpaQueryFactory
                .select(board.id)
                .from(board)
                .where(
                        board.completed.isTrue(),
                        eqMaxPerson(condition.maxPerson()),
                        inPreferredTeams(condition.preferredTeamIds()),
                        inMatchingGames(condition.matchingGameIds()),
                        notInBlockedUsers(condition.blockedUserIds()),
                        eqWriter(condition.writerId()),
                        afterCursor(condition.lastLiftUpDate(), condition.lastBoardId()))
                .orderBy(board.liftUpDate.desc(), board.id.desc())
                .limit(limit)
                .fetch();
        if (boardIds.isEmpty()) {
            return List.of();
        }
        return jpaQueryFactory
                .selectFrom(board)
                .where(board.id.in(boardIds))
                .orderBy(board.liftUpDate.desc(), board.id.desc())
                .fetch();
    }

    @Override
    public List<Board> findAllPublished(long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(board)
                .where(board.completed.isTrue())
                .orderBy(board.createdAt.desc(), board.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countPublished() {
        return boardJpaRepository.countByCompletedTrue();
    }

    @Override
    public List<Board> findAllByWriterId(Long writerId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(board)
                .where(board.userId.eq(writerId))
                .orderBy(board.liftUpDate.desc(), board.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByWriterId(Long writerId) {
        return boardJpaRepository.countByUserId(writerId);
    }

    @Override
    public long count() {
        return boardJpaRepository.count();
    }

    private BooleanExpression eqMaxPerson(Integer maxPerson) {
        return maxPerson != null ? board.maxPerson.eq(maxPerson) : null;
    }

    private BooleanExpression inPreferredTeams(List<Long> teamIds) {
        return teamIds.isEmpty() ? null : board.cheerClubId.in(teamIds);
    }

    private BooleanExpression inMatchingGames(List<Long> gameIds) {
        return gameIds == null || gameIds.isEmpty() ? null : board.gameId.in(gameIds);
    }

    private BooleanExpression notInBlockedUsers(List<Long> blockedUserIds) {
        return blockedUserIds.isEmpty() ? null : board.userId.notIn(blockedUserIds);
    }

    private BooleanExpression eqWriter(Long writerId) {
        return writerId != null ? board.userId.eq(writerId) : null;
    }

    private BooleanExpression afterCursor(LocalDateTime lastLiftUpDate, Long lastBoardId) {
        if (lastLiftUpDate == null || lastBoardId == null) {
            return null;
        }
        return board.liftUpDate
                .lt(lastLiftUpDate)
                .or(board.liftUpDate.eq(lastLiftUpDate).and(board.id.lt(lastBoardId)));
    }
}
