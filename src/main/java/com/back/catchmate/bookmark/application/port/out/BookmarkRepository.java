package com.back.catchmate.bookmark.application.port.out;

import com.back.catchmate.bookmark.domain.model.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository {
    Bookmark save(Bookmark bookmark);

    Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId);

    Page<Bookmark> findAllByUserId(Long userId, Pageable pageable);

    List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds);

    boolean existsByUserIdAndBoardId(Long userId, Long boardId);

    void delete(Bookmark bookmark);
}
