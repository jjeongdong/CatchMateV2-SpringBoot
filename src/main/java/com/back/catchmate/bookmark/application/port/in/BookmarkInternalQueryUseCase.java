package com.back.catchmate.bookmark.application.port.in;

import java.util.List;

public interface BookmarkInternalQueryUseCase {
    boolean isBookmarked(Long userId, Long boardId);

    List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds);
}
