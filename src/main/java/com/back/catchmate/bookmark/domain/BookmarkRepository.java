package com.back.catchmate.bookmark.domain;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface BookmarkRepository {

    // 이미 찜했으면 아무것도 하지 않는다 (멱등 PUT).
    void saveIfAbsent(Bookmark bookmark);

    // 찜하지 않았으면 아무것도 하지 않는다 (멱등 DELETE).
    void deleteByUserIdAndBoardId(Long userId, Long boardId);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    Set<Long> findBookmarkedBoardIds(Long userId, Collection<Long> boardIds);

    // 최근 찜한 순 (등록 시각 내림차순 → id 내림차순)
    List<Bookmark> findAllByUserId(Long userId, long offset, int limit);

    long countByUserId(Long userId);
}
