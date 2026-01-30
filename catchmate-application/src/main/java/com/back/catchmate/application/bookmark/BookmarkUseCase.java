package com.back.catchmate.application.bookmark;

import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.domain.bookmark.service.BookmarkService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkUseCase {
    private final BookmarkService bookmarkService;
    private final BoardService boardService;
    private final UserService userService;

    public PagedResponse<BoardResponse> getBookmarkList(Long userId, int page, int size) {
        // 도메인 페이징 객체 생성
        DomainPageable pageable = DomainPageable.of(page, size);

        // 사용자가 찜한 게시글 목록 조회
        DomainPage<Bookmark> bookmarkPage = bookmarkService.getBookmarkList(userId, pageable);

        // Bookmark 객체를 BoardResponse DTO로 변환
        List<BoardResponse> boardResponses = bookmarkPage.getContent().stream()
                .map(bookmark -> BoardResponse.of(bookmark.getBoard(), true))
                .toList();

        // 페이징 응답 생성 및 반환
        return new PagedResponse<>(bookmarkPage, boardResponses);
    }

    @Transactional
    public void updateBookmark(Long userId, Long boardId) {
        // 게시글 존재 확인
        Board board = boardService.getBoard(boardId);
        User user = userService.getUser(userId);

        // 찜 여부 확인 후 토글 처리
        bookmarkService.updateBookmark(user, board);
    }
}
