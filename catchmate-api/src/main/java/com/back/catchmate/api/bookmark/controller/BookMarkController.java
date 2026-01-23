package com.back.catchmate.api.bookmark.controller;

import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.bookmark.BookmarkUseCase;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
@Tag(name = "[사용자] 찜하기 API")
public class BookMarkController {
    private final BookmarkUseCase bookmarkUseCase;

    @PostMapping("/{boardId}")
    @Operation(summary = "게시글 찜하기/취소", description = "게시글을 찜하거나 찜을 취소합니다. (토글 방식)")
    public ResponseEntity<Void> toggleBookmark(@AuthUser Long userId,
                                               @PathVariable Long boardId) {
        bookmarkUseCase.toggleBookmark(userId, boardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "내가 찜한 게시글 목록 조회", description = "내가 찜한 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<BoardResponse>> getMyBookmarks(@AuthUser Long userId,
                                                                       @RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BoardResponse> response = bookmarkUseCase.getMyBookmarks(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
