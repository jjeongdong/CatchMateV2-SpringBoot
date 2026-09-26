package com.back.catchmate.bookmark.application;

import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.bookmark.domain.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkCommandService {
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public void createBookmark(Long userId, Long boardId) {
        bookmarkRepository.saveIfAbsent(Bookmark.create(userId, boardId));
    }

    @Transactional
    public void deleteBookmark(Long userId, Long boardId) {
        bookmarkRepository.deleteByUserIdAndBoardId(userId, boardId);
    }
}
