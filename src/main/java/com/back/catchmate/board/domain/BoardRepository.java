package com.back.catchmate.board.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BoardRepository {

    Board save(Board board);

    // 임시저장 글도 포함. 없으면 BoardNotFoundException
    Board getById(Long boardId);

    // 발행 글만. 없으면 BoardNotFoundException
    Board getPublishedById(Long boardId);

    List<Board> findAllByIds(Collection<Long> boardIds);

    Optional<Board> findDraftByWriterId(Long writerId);

    void deleteDraft(Board draft);

    // 발행 글, 끌어올린 시각 내림차순 → id 내림차순
    List<Board> findAllByCondition(BoardSearchCondition condition, int limit);

    // 관리자 전체 목록: 발행 글, 등록 시각 내림차순 → id 내림차순
    List<Board> findAllPublished(long offset, int limit);

    long countPublished();

    // 관리자 유저별 목록: 임시저장 포함, 끌어올린 시각 내림차순 → id 내림차순
    List<Board> findAllByWriterId(Long writerId, long offset, int limit);

    long countByWriterId(Long writerId);

    long count();
}
