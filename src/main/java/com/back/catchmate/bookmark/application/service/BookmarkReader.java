package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.bookmark.application.port.out.persistence.BookmarkRepository;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookmarkReader {
    private final BookmarkRepository bookmarkRepository;

    public Optional<Bookmark> findByUserIdAndBoardId(Long userId, Long boardId) {
        return bookmarkRepository.findByUserIdAndBoardId(userId, boardId);
    }

    public Page<Bookmark> findAllByUserId(Long userId, Pageable pageable) {
        return bookmarkRepository.findAllByUserId(userId, pageable);
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return bookmarkRepository.findBookmarkedBoardIds(userId, boardIds);
    }
}
