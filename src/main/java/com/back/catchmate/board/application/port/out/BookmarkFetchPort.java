package com.back.catchmate.board.application.port.out;

import java.util.List;

public interface BookmarkFetchPort {
    boolean isBookmarked(Long userId, Long boardId);
    List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds);
}
