package com.back.catchmate.enroll.application.port.out;

public interface BookmarkFetchPort {
    boolean isBookmarked(Long userId, Long boardId);
}
