package com.back.catchmate.domain.bookmark.repository;

import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.Optional;

public interface BookmarkRepository {
    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId);

    DomainPage<Bookmark> findAllByUserId(Long userId, DomainPageable pageable);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    void delete(Bookmark bookmark);
}
