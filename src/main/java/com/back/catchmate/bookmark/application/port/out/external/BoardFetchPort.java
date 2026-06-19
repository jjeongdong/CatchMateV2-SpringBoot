package com.back.catchmate.bookmark.application.port.out.external;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkBoardInfo;

import java.util.List;

public interface BoardFetchPort {
    List<BookmarkBoardInfo> getBoards(List<Long> boardIds);
}
