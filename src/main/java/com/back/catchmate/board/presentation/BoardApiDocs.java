package com.back.catchmate.board.presentation;

import com.back.catchmate.board.application.dto.result.BoardCreateResult;
import com.back.catchmate.board.application.dto.result.BoardDetailResult;
import com.back.catchmate.board.application.dto.result.BoardDraftResult;
import com.back.catchmate.board.application.dto.result.BoardLiftUpResult;
import com.back.catchmate.board.application.dto.result.BoardResult;
import com.back.catchmate.board.application.dto.result.BoardUpdateResult;
import com.back.catchmate.board.presentation.dto.request.BoardCreateRequest;
import com.back.catchmate.board.presentation.dto.request.BoardUpdateRequest;
import com.back.catchmate.global.response.CursorPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;

// 검증 어노테이션은 여기에만 둔다 (구현 메서드에 두면 HV000151).
@Tag(name = "[사용자] 게시글 관련 API")
public interface BoardApiDocs {

    @Operation(summary = "게시글 생성/임시저장 API", description = "게시글을 생성하거나 임시저장합니다. 이전 임시저장 글은 지워집니다. (201)")
    ResponseEntity<BoardCreateResult> createBoard(
            @Parameter(hidden = true) Long userId, @Valid BoardCreateRequest request);

    @Operation(summary = "게시글 단일 조회 API", description = "게시글 ID로 상세 정보를 조회합니다.")
    ResponseEntity<BoardDetailResult> getBoard(@Parameter(hidden = true) Long userId, Long boardId);

    @Operation(summary = "임시저장된 게시글 조회 API", description = "내 임시저장 글을 조회합니다. 없으면 204 를 돌려줍니다.")
    ResponseEntity<BoardDraftResult> getMyDraft(@Parameter(hidden = true) Long userId);

    @Operation(
            summary = "게시글 목록 조회 (무한스크롤)",
            description = "첫 페이지는 cursor 없이 요청하고, 이후 응답의 nextCursor 를 cursor 로 전달합니다.")
    ResponseEntity<CursorPageResult<BoardResult>> getBoards(
            @Parameter(hidden = true) Long userId,
            LocalDate gameDate,
            Integer maxPerson,
            List<Long> preferredTeamIds,
            String cursor,
            @Min(1) @Max(100) int size);

    @Operation(summary = "유저별 게시글 조회", description = "특정 유저의 게시글을 끌어올린 순으로 조회합니다. 내가 차단한 유저면 400.")
    ResponseEntity<CursorPageResult<BoardResult>> getUserBoards(
            Long userId, @Parameter(hidden = true) Long loginUserId, String cursor, @Min(1) @Max(100) int size);

    @Operation(summary = "게시글 수정 API", description = "신청자가 합류한 뒤에는 제목·내용만 수정할 수 있습니다.")
    ResponseEntity<BoardUpdateResult> updateBoard(
            @Parameter(hidden = true) Long userId, Long boardId, @Valid BoardUpdateRequest request);

    @Operation(summary = "게시글 끌어올리기 API", description = "마지막 끌어올리기 후 3일이 지나야 합니다. 아직이면 liftedUp=false 와 남은 시간을 돌려줍니다.")
    ResponseEntity<BoardLiftUpResult> liftUpBoard(@Parameter(hidden = true) Long userId, Long boardId);

    @Operation(summary = "게시글 삭제 API", description = "게시글을 삭제합니다. (204)")
    ResponseEntity<Void> deleteBoard(@Parameter(hidden = true) Long userId, Long boardId);
}
