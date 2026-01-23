package com.back.catchmate.domain.bookmark.repository;

import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;

import java.util.Optional;

public interface BookmarkRepository {
    Bookmark save(Bookmark bookmark);
    void delete(Bookmark bookmark);
    Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId);
    boolean existsByUserIdAndBoardId(Long userId, Long boardId);
    DomainPage<Bookmark> findAllByUserId(Long userId, DomainPageable pageable);
}
