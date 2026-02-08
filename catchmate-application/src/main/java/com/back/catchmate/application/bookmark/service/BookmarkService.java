package com.back.catchmate.application.bookmark.service;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.bookmark.repository.BookmarkRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

    public void createBookmark(Bookmark bookmark) {
        bookmarkRepository.save(bookmark);
    }

    public void deleteBookmark(Bookmark bookmark) {
        bookmarkRepository.delete(bookmark);
    }

    public Optional<Bookmark> findByUserAndBoard(User user, Board board) {
        return bookmarkRepository.findByUserIdAndBoardId(user.getId(), board.getId());
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    public DomainPage<Bookmark> findAllByUser(User user, DomainPageable pageable) {
        return bookmarkRepository.findAllByUserId(user.getId(), pageable);
    }
}
