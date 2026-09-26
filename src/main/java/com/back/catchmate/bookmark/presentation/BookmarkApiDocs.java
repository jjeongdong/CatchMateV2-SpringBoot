package com.back.catchmate.bookmark.presentation;

import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult;
import com.back.catchmate.global.response.OffsetPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[사용자] 찜 관련 API")
public interface BookmarkApiDocs {

    @Operation(summary = "찜하기 API", description = "게시글을 찜합니다. 이미 찜했어도 204 입니다.")
    ResponseEntity<Void> createBookmark(@Parameter(hidden = true) Long userId, Long boardId);

    @Operation(summary = "찜 취소 API", description = "게시글 찜을 취소합니다. 찜하지 않았어도 204 입니다.")
    ResponseEntity<Void> deleteBookmark(@Parameter(hidden = true) Long userId, Long boardId);

    @Operation(summary = "찜한 목록 조회 API", description = "내가 찜한 게시글을 최근 찜한 순으로 조회합니다.")
    ResponseEntity<OffsetPageResult<BookmarkedBoardResult>> getBookmarks(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);
}
