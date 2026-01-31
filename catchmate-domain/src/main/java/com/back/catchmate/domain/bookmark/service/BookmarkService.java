package com.back.catchmate.domain.bookmark.service;

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

    public DomainPage<Bookmark> getBookmarkList(Long userId, DomainPageable pageable) {
        return bookmarkRepository.findAllByUserId(userId, pageable);
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    public boolean updateBookmark(User user, Board board) {
        Optional<Bookmark> existingBookmark = bookmarkRepository.findByUserIdAndBoardId(user.getId(), board.getId());

        if (existingBookmark.isPresent()) {
            bookmarkRepository.delete(existingBookmark.get());
            return false; // 찜 취소됨
        } else {
            Bookmark newBookmark = Bookmark.createBookmark(user, board);
            bookmarkRepository.save(newBookmark);
            return true; // 찜 추가됨
        }
    }
}
