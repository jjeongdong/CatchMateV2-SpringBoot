package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.bookmark.application.port.out.UserFetchPort;

import com.back.catchmate.bookmark.application.port.out.BoardFetchPort;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.bookmark.application.port.in.BookmarkUseCase;
import com.back.catchmate.bookmark.application.port.out.BookmarkRepository;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import com.back.catchmate.common.response.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkService implements BookmarkUseCase {

    private final BookmarkRepository bookmarkRepository;

    private final BoardFetchPort boardFetchPort;
    private final UserFetchPort userFetchPort;

    @Transactional
    public BookmarkUpdateResponse updateBookmark(Long userId, Long boardId) {
        User user = userFetchPort.getUser(userId);
        Board board = boardFetchPort.getBoard(boardId);

        Optional<Bookmark> bookmarkOptional = findByUserAndBoard(user, board);

        if (bookmarkOptional.isPresent()) {
            deleteBookmark(bookmarkOptional.get());
            return new BookmarkUpdateResponse(boardId,false);
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .board(board)
                    .build();
            createBookmark(bookmark);
            return new BookmarkUpdateResponse(boardId,true);
        }
    }

    public PagedResponse<BoardResponse> getBookmarkedBoards(Long userId, int page, int size) {
        User user = userFetchPort.getUser(userId);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Bookmark> bookmarkPage = findAllByUser(user, pageable);

        List<BoardResponse> boardResponses = bookmarkPage.getContent().stream()
                .map(bookmark -> BoardResponse.from(bookmark.getBoard(), true))
                .toList();

        return new PagedResponse<>(bookmarkPage, boardResponses);
    }

    public void createBookmark(Bookmark bookmark) {
        bookmarkRepository.save(bookmark);
    }

    public void deleteBookmark(Bookmark bookmark) {
        bookmarkRepository.delete(bookmark);
    }

    public Optional<Bookmark> findByUserAndBoard(User user, Board board) {
        return bookmarkRepository.findByUserIdAndBoardId(user.getId(), board.getId());
    }

    public Page<Bookmark> findAllByUser(User user, Pageable pageable) {
        return bookmarkRepository.findAllByUserId(user.getId(), pageable);
    }

    public List<Long> findBookmarkedBoardIds(User user, List<Long> boardIds) {
        return bookmarkRepository.findBookmarkedBoardIds(user.getId(), boardIds);
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }
}
