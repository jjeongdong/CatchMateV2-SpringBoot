package com.back.catchmate.orchestration.bookmark;

import com.back.catchmate.application.board.service.BoardService;
import com.back.catchmate.application.bookmark.service.BookmarkService;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.bookmark.dto.response.BookmarkUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BookmarkOrchestrator {
    private final BookmarkService bookmarkService;
    private final UserService userService;
    private final BoardService boardService;

    @Transactional
    public BookmarkUpdateResponse updateBookmark(Long userId, Long boardId) {
        User user = userService.getUser(userId);
        Board board = boardService.getBoard(boardId);

        Optional<Bookmark> bookmarkOptional = bookmarkService.findByUserAndBoard(user, board);

        if (bookmarkOptional.isPresent()) {
            bookmarkService.deleteBookmark(bookmarkOptional.get());
            return new BookmarkUpdateResponse(boardId,false);
        } else {
            Bookmark bookmark = Bookmark.builder()
                    .user(user)
                    .board(board)
                    .build();
            bookmarkService.createBookmark(bookmark);
            return new BookmarkUpdateResponse(boardId,true);
        }
    }

    @Transactional(readOnly = true)
    public PagedResponse<BoardResponse> getBookmarkedBoards(Long userId, int page, int size) {
        User user = userService.getUser(userId);
        DomainPageable pageable = DomainPageable.of(page, size);
        
        DomainPage<Bookmark> bookmarkPage = bookmarkService.findAllByUser(user, pageable);

        List<BoardResponse> boardResponses = bookmarkPage.getContent().stream()
                .map(bookmark -> BoardResponse.from(bookmark.getBoard(), true))
                .toList();

        return new PagedResponse<>(bookmarkPage, boardResponses);
    }
}
