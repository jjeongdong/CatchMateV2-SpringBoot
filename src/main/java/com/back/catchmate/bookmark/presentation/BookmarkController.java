package com.back.catchmate.bookmark.presentation;

import com.back.catchmate.bookmark.application.BookmarkCommandService;
import com.back.catchmate.bookmark.application.BookmarkQueryService;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.OffsetPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController implements BookmarkApiDocs {
    private final BookmarkCommandService bookmarkCommandService;
    private final BookmarkQueryService bookmarkQueryService;

    @Override
    @PutMapping("/{boardId}")
    public ResponseEntity<Void> createBookmark(@AuthUser Long userId, @PathVariable Long boardId) {
        bookmarkCommandService.createBookmark(userId, boardId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBookmark(@AuthUser Long userId, @PathVariable Long boardId) {
        bookmarkCommandService.deleteBookmark(userId, boardId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<BookmarkedBoardResult>> getBookmarks(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookmarkQueryService.getBookmarks(userId, page, size));
    }
}
