package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.bookmark.application.service.BookmarkService;
import com.back.catchmate.enroll.application.port.out.BookmarkFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollBookmarkFetchAdapter implements BookmarkFetchPort {
    private final BookmarkService bookmarkService;

    @Override
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkService.isBookmarked(userId, boardId);
    }
}
