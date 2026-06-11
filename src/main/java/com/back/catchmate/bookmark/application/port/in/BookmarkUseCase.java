package com.back.catchmate.bookmark.application.port.in;

import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.common.orchestration.PagedResponse;

public interface BookmarkUseCase {
    BookmarkUpdateResponse updateBookmark(Long userId, Long boardId);
    PagedResponse<BoardResponse> getBookmarkedBoards(Long userId, int page, int size);
}
