package com.back.catchmate.application.bookmark.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BookmarkUpdateResponse {
    private final Long boardId;
    private final boolean bookmarked;

    public static BookmarkUpdateResponse of(Long boardId, boolean bookmarked) {
        return new BookmarkUpdateResponse(boardId, bookmarked);
    }
}
