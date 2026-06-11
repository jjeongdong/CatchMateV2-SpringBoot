package com.back.catchmate.board.application.port.out;

import com.back.catchmate.user.domain.model.User;

import java.util.List;

public interface BookmarkFetchPort {
    boolean isBookmarked(Long userId, Long boardId);
    List<Long> findBookmarkedBoardIds(User user, List<Long> boardIds);
}
