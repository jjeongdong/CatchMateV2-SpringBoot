package com.back.catchmate.bookmark.dto.response;

public record BookmarkUpdateResponse(Long boardId, boolean bookmarked) {
    public static BookmarkUpdateResponse of(Long boardId, boolean bookmarked) {
        return new BookmarkUpdateResponse(boardId, bookmarked);
    }
}
