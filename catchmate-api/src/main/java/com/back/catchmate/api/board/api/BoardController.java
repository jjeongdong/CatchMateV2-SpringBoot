package com.back.catchmate.api.board.api;

import com.back.catchmate.api.board.dto.request.BoardCreateRequest;
import com.back.catchmate.api.board.dto.request.BoardUpdateRequest;
import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.authorization.annotation.CheckBoardPermission;
import com.back.catchmate.authorization.annotation.PermissionId;
import com.back.catchmate.orchestration.board.BoardOrchestrator;
import com.back.catchmate.orchestration.board.dto.response.BoardCreateResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardDetailResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardLiftUpResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardTempDetailResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardUpdateResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
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
    private final BoardOrchestrator boardOrchestrator;

    @PostMapping
    @Operation(summary = "게시글 생성/임시저장 API", description = "게시글을 생성하거나 임시저장합니다.")
    public ResponseEntity<BoardCreateResponse> createBoard(@AuthUser Long userId,
                                                           @Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.ok(boardOrchestrator.createBoard(userId, request.toCommand()));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "게시글 단일 조회 API", description = "게시글 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<BoardDetailResponse> getBoard(@AuthUser Long userId,
                                                        @PathVariable Long boardId) {
        return ResponseEntity.ok(boardOrchestrator.getBoard(userId, boardId));
    }

    @GetMapping("/temp")
    @Operation(summary = "임시저장된 게시글 단일 조회 API")
    public ResponseEntity<BoardTempDetailResponse> getTempBoard(@AuthUser Long userId) {
        BoardTempDetailResponse response = boardOrchestrator.getTempBoard(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회")
    public ResponseEntity<PagedResponse<BoardResponse>> getBoardList(@Parameter(hidden = true) @AuthUser Long userId,
                                                                     @RequestParam(required = false) LocalDate gameDate,
                                                                     @RequestParam(required = false) Integer maxPerson,
                                                                     @RequestParam(required = false) List<Long> preferredTeamIdList,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BoardResponse> response = boardOrchestrator.getBoardList(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                page,
                size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "유저별 게시글 조회")
    @GetMapping("/users/{userId}")
    public ResponseEntity<PagedResponse<BoardResponse>> getBoardListByUserId(@PathVariable Long userId,
                                                                             @Parameter(hidden = true) @AuthUser Long loginUserId,
                                                                             @RequestParam(defaultValue = "0") int page,
                                                                             @RequestParam(defaultValue = "10") int size) {
        PagedResponse<BoardResponse> response = boardOrchestrator.getBoardListByUserId(userId, loginUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @CheckBoardPermission
    @PutMapping("/{boardId}")
    @Operation(summary = "게시글 수정 API")
    public ResponseEntity<BoardUpdateResponse> updateBoard(@AuthUser Long userId,
                                                           @PermissionId @PathVariable Long boardId,
                                                           @Valid @RequestBody BoardUpdateRequest request) {
        return ResponseEntity.ok(boardOrchestrator.updateBoard(userId, boardId, request.toCommand()));
    }

    @CheckBoardPermission
    @PatchMapping("/{boardId}/lift-up")
    @Operation(summary = "게시글 끌어올리기 API")
    public ResponseEntity<BoardLiftUpResponse> updateLiftUpDate(@AuthUser Long userId,
                                                                @PermissionId @PathVariable Long boardId) {
        return ResponseEntity.ok(boardOrchestrator.updateLiftUpDate(userId, boardId));
    }

    @CheckBoardPermission
    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시글 삭제 API")
    public ResponseEntity<Void> deleteBoard(@AuthUser Long userId,
                                            @PermissionId @PathVariable Long boardId) {
        boardOrchestrator.deleteBoard(userId, boardId);
        return ResponseEntity.ok().build();
    }
}
