package com.back.catchmate.bookmark.application.port.in;

import com.back.catchmate.bookmark.application.dto.response.BookmarkedBoardSummary;
import com.back.catchmate.common.response.PagedResponse;

public interface BookmarkClientQueryUseCase {
    PagedResponse<BookmarkedBoardSummary> getBookmarkedBoards(Long userId, int page, int size);
}
