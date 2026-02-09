package com.back.catchmate.api.bookmark.controller;

import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.bookmark.BookmarkOrchestrator;
import com.back.catchmate.orchestration.bookmark.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "[사용자] 찜 관련 API")
@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookMarkController {
    private final BookmarkOrchestrator bookmarkOrchestrator;

    @PostMapping("/{boardId}")
    @Operation(summary = "찜 등록/취소 API", description = "게시글을 찜하거나 찜을 취소합니다.")
    public ResponseEntity<BookmarkUpdateResponse> updateBookmark(@AuthUser Long userId,
                                                                 @PathVariable Long boardId) {
        return ResponseEntity.ok(bookmarkOrchestrator.updateBookmark(userId, boardId));
    }

    @GetMapping
    @Operation(summary = "찜한 목록 조회 API", description = "내가 찜한 게시글 목록을 조회합니다.")
    public ResponseEntity<PagedResponse<BoardResponse>> getBookmarkedBoards(@Parameter(hidden = true) @AuthUser Long userId,
                                                                            @RequestParam(defaultValue = "0") int page,
                                                                            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BoardResponse> response = bookmarkOrchestrator.getBookmarkedBoards(userId, page, size);
        return ResponseEntity.ok(response);
    }
}
