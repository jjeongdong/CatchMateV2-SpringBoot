package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.bookmark.application.port.in.BookmarkUseCase;
import com.back.catchmate.bookmark.application.port.out.BoardFetchPort;
import com.back.catchmate.bookmark.application.port.out.BookmarkRepository;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import com.back.catchmate.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkService implements BookmarkUseCase {

    private final BookmarkRepository bookmarkRepository;

    private final BoardFetchPort boardFetchPort;

    @Override
    @Transactional
    public BookmarkUpdateResponse updateBookmark(Long userId, Long boardId) {
        Optional<Bookmark> bookmarkOptional =
                bookmarkRepository.findByUserIdAndBoardId(userId, boardId);

        if (bookmarkOptional.isPresent()) {
            bookmarkRepository.delete(bookmarkOptional.get());
            return new BookmarkUpdateResponse(boardId, false);
        }

        bookmarkRepository.save(Bookmark.createBookmark(userId, boardId));
        return new BookmarkUpdateResponse(boardId, true);
    }

    @Override
    public PagedResponse<BoardResponse> getBookmarkedBoards(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkRepository.findAllByUserId(userId, pageable);

        List<Long> boardIds = bookmarkPage.getContent().stream()
                .map(Bookmark::getBoardId)
                .toList();
        // Bulk fetch: avoid N+1 by getting all boards in one shot.
        List<Board> boards = boardFetchPort.getBoards(boardIds);

        List<BoardResponse> boardResponses = boards.stream()
                .map(board -> BoardResponse.from(board, true))
                .toList();

        return new PagedResponse<>(bookmarkPage, boardResponses);
    }

    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        return bookmarkRepository.findBookmarkedBoardIds(userId, boardIds);
    }

    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }
}
