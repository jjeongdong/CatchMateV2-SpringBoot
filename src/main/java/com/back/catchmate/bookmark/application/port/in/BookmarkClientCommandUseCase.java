package com.back.catchmate.bookmark.application.port.in;

import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;

public interface BookmarkClientCommandUseCase {
    BookmarkUpdateResponse updateBookmark(Long userId, Long boardId);
}
