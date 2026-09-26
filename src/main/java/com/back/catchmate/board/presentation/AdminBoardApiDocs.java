package com.back.catchmate.board.presentation;

import com.back.catchmate.board.application.dto.result.AdminBoardDetailResult;
import com.back.catchmate.board.application.dto.result.AdminBoardResult;
import com.back.catchmate.global.response.OffsetPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[관리자] 게시글 관련 API")
public interface AdminBoardApiDocs {

    @Operation(
            summary = "관리자 게시글 목록 조회",
            description = "userId 가 없으면 발행된 전체 게시글을 최신 등록순으로, 있으면 그 유저의 게시글(임시저장 포함)을 끌어올린 순으로 조회합니다.")
    ResponseEntity<OffsetPageResult<AdminBoardResult>> getAdminBoards(
            Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(summary = "관리자 게시글 상세 조회", description = "게시글 상세 정보와 대기 중인 신청자 목록을 조회합니다.")
    ResponseEntity<AdminBoardDetailResult> getAdminBoard(Long boardId);
}
