package com.back.catchmate.bookmark.application.port.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkGameInfo;

import java.util.List;

public interface GameFetchPort {
    List<BookmarkGameInfo> getGames(List<Long> gameIds);
}
