package com.back.catchmate.api.board.api;

import com.back.catchmate.api.board.dto.request.BoardCreateRequest;
import com.back.catchmate.api.board.dto.request.BoardUpdateRequest;
import com.back.catchmate.application.board.BoardUseCase;
import com.back.catchmate.application.board.dto.response.BoardDetailResponse;
import com.back.catchmate.application.board.dto.response.BoardLiftUpResponse;
import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.board.dto.response.BoardTempDetailResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.common.permission.PermissionId;
import com.back.catchmate.global.annotation.AuthUser;
import com.back.catchmate.global.aop.permission.CheckBoardPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "[사용자] 게시글 관련 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;

    @PostMapping
    @Operation(summary = "게시글 생성/임시저장 API", description = "게시글을 생성하거나 임시저장합니다. (isCompleted: true=게시, false=임시저장)")
    public ResponseEntity<BoardResponse> createBoard(@AuthUser Long userId,
                                                     @Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.ok(boardUseCase.createBoard(userId, request.toCommand()));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "게시글 단일 조회 API", description = "게시글 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<BoardDetailResponse> getBoardDetail(@AuthUser Long userId,
                                                        @PathVariable Long boardId) {
        return ResponseEntity.ok(boardUseCase.getBoardDetail(userId, boardId));
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회", description = "조건에 맞는 게시글 목록을 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<BoardResponse>> getBoardList(
            @Parameter(hidden = true) @AuthUser Long userId,
            @Parameter(description = "경기 날짜 (YYYY-MM-DD)") @RequestParam(required = false) LocalDate gameDate,
            @Parameter(description = "최대 인원 수") @RequestParam(required = false) Integer maxPerson,
            @Parameter(description = "선호 구단 ID 목록 (예: 1,2,3)") @RequestParam(required = false) List<Long> preferredTeamIdList,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<BoardResponse> response = boardUseCase.getBoardList(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                page,
                size
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유저별 게시글 조회", description = "특정 유저가 작성한 모든 게시글을 조회합니다.")
    @GetMapping("/users/{userId}")
    public ResponseEntity<PagedResponse<BoardResponse>> getBoardListByUserId(
            @Parameter(description = "조회 대상 유저 ID") @PathVariable Long userId,
            @Parameter(hidden = true) @AuthUser Long loginUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PagedResponse<BoardResponse> response = boardUseCase.getBoardListByUserId(userId, loginUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/temp")
    @Operation(summary = "임시저장된 게시글 단일 조회 API", description = "사용자의 임시저장된 게시글을 조회합니다. 존재하지 않으면 null을 반환합니다.")
    public ResponseEntity<BoardTempDetailResponse> getTempBoard(@AuthUser Long userId) {
        BoardTempDetailResponse response = boardUseCase.getTempBoard(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    @CheckBoardPermission
    @PutMapping("/{boardId}")
    @Operation(summary = "게시글 수정 API", description = "게시글을 수정합니다.")
    public ResponseEntity<BoardResponse> updateBoard(@AuthUser Long userId,
                                                     @PathVariable @PermissionId Long boardId,
                                                     @Valid @RequestBody BoardUpdateRequest request) {
        return ResponseEntity.ok(boardUseCase.updateBoard(userId, boardId, request.toCommand()));
    }

    @CheckBoardPermission
    @PatchMapping("/{boardId}/lift-up")
    @Operation(summary = "게시글 끌어올리기 API", description = "게시글을 끌어올립니다.")
    public ResponseEntity<BoardLiftUpResponse> updateLiftUpDate(@AuthUser Long userId,
                                                @PathVariable @PermissionId Long boardId) {
        return ResponseEntity.ok(boardUseCase.updateLiftUpDate(userId, boardId));
    }

    @CheckBoardPermission
    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시글 삭제 API", description = "게시글을 삭제합니다.")
    public ResponseEntity<Void> deleteBoard(@AuthUser Long userId,
                                            @PathVariable @PermissionId Long boardId) {
        boardUseCase.deleteBoard(userId, boardId);
        return ResponseEntity.ok().build();
    }
}
