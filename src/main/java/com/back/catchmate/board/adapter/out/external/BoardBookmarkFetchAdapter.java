package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.BookmarkFetchPort;
import com.back.catchmate.bookmark.application.service.BookmarkService;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardBookmarkFetchAdapter implements BookmarkFetchPort {
    private final BookmarkService bookmarkService;

    @Override
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkService.isBookmarked(userId, boardId);
    }

    @Override
    public List<Long> findBookmarkedBoardIds(User user, List<Long> boardIds) {
        return bookmarkService.findBookmarkedBoardIds(user, boardIds);
    }
}
