package com.back.catchmate.domain.bookmark.repository;

import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository {
    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId);

    DomainPage<Bookmark> findAllByUserId(Long userId, DomainPageable pageable);

    List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    void delete(Bookmark bookmark);
}
