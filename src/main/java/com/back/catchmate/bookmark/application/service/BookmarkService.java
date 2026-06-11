package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import com.back.catchmate.bookmark.application.port.out.BookmarkRepository;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public DomainPage<Bookmark> findAllByUser(User user, DomainPageable pageable) {
        return bookmarkRepository.findAllByUserId(user.getId(), pageable);
    }

    public List<Long> findBookmarkedBoardIds(User user, List<Long> boardIds) {
        return bookmarkRepository.findBookmarkedBoardIds(user.getId(), boardIds);
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }
}
