package com.back.catchmate.bookmark.adapter.in.web.controller;

import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.bookmark.application.port.in.BookmarkClientCommandUseCase;
import com.back.catchmate.bookmark.application.port.in.BookmarkClientQueryUseCase;
import com.back.catchmate.bookmark.application.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.bookmark.application.dto.response.BookmarkedBoardSummary;
import com.back.catchmate.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 찜 관련 API")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookMarkController {
    private final BookmarkClientCommandUseCase bookmarkClientCommandUseCase;
    private final BookmarkClientQueryUseCase bookmarkClientQueryUseCase;

    @PostMapping("/{boardId}")
    @Operation(summary = "찜 등록/취소 API", description = "게시글을 찜하거나 찜을 취소합니다.")
    public ResponseEntity<BookmarkUpdateResponse> updateBookmark(@AuthUser Long userId,
                                                                 @PathVariable Long boardId) {
        return ResponseEntity.ok(bookmarkClientCommandUseCase.updateBookmark(userId, boardId));
    }

    @GetMapping
    @Operation(summary = "찜한 목록 조회 API", description = "내가 찜한 게시글 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<BookmarkedBoardSummary>> getBookmarkedBoards(@Parameter(hidden = true) @AuthUser Long userId,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookmarkClientQueryUseCase.getBookmarkedBoards(userId, page, size));
    }
}
