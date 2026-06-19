package com.back.catchmate.board.adapter.in.web.controller;

import com.back.catchmate.board.adapter.in.web.dto.request.BoardCreateRequest;
import com.back.catchmate.board.adapter.in.web.dto.request.BoardUpdateRequest;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.application.port.in.BoardClientCommandUseCase;
import com.back.catchmate.board.application.port.in.BoardClientQueryUseCase;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "[사용자] 게시글 관련 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardClientCommandUseCase boardClientCommandUseCase;
    private final BoardClientQueryUseCase boardClientQueryUseCase;

    @PostMapping
    @Operation(summary = "게시글 생성/임시저장 API", description = "게시글을 생성하거나 임시저장합니다.")
    public ResponseEntity<BoardCreateResponse> createBoard(@AuthUser Long userId,
                                                           @Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.ok(boardClientCommandUseCase.createBoard(userId, request.toCommand()));
    }

    @GetMapping("/{boardId}")
    @Operation(summary = "게시글 단일 조회 API", description = "게시글 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<BoardDetailResponse> getBoard(@AuthUser Long userId,
                                                        @PathVariable Long boardId) {
        return ResponseEntity.ok(boardClientQueryUseCase.getBoard(userId, boardId));
    }

    @GetMapping("/temp")
    @Operation(summary = "임시저장된 게시글 단일 조회 API")
    public ResponseEntity<BoardTempDetailResponse> getTempBoard(@AuthUser Long userId) {
        BoardTempDetailResponse response = boardClientQueryUseCase.getTempBoard(userId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "게시글 목록 조회 (무한스크롤)",
            description = "첫 페이지는 커서 없이 요청. 이후 응답의 nextCursorDateTime, nextCursorId를 파라미터로 전달.")
    public ResponseEntity<CursorPagedResponse<BoardResponse>> getBoardList(
            @Parameter(hidden = true) @AuthUser Long userId,
            @RequestParam(required = false) LocalDate gameDate,
            @RequestParam(required = false) Integer maxPerson,
            @RequestParam(required = false) List<Long> preferredTeamIdList,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastLiftUpDate,
            @RequestParam(required = false) Long lastBoardId,
            @RequestParam(defaultValue = "10") int size) {
        CursorPagedResponse<BoardResponse> response = boardClientQueryUseCase.getBoardList(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                lastLiftUpDate,
                lastBoardId,
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
        PagedResponse<BoardResponse> response = boardClientQueryUseCase.getBoardListByUserId(userId, loginUserId, page, size);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{boardId}")
    @Operation(summary = "게시글 수정 API")
    public ResponseEntity<BoardUpdateResponse> updateBoard(@AuthUser Long userId,
                                                           @PathVariable Long boardId,
                                                           @Valid @RequestBody BoardUpdateRequest request) {
        return ResponseEntity.ok(boardClientCommandUseCase.updateBoard(userId, boardId, request.toCommand()));
    }

    @PatchMapping("/{boardId}/lift-up")
    @Operation(summary = "게시글 끌어올리기 API")
    public ResponseEntity<BoardLiftUpResponse> updateLiftUpDate(@AuthUser Long userId,
                                                                @PathVariable Long boardId) {
        return ResponseEntity.ok(boardClientCommandUseCase.updateLiftUpDate(userId, boardId));
    }

    @DeleteMapping("/{boardId}")
    @Operation(summary = "게시글 삭제 API")
    public ResponseEntity<Void> deleteBoard(@AuthUser Long userId,
                                            @PathVariable Long boardId) {
        boardClientCommandUseCase.deleteBoard(userId, boardId);
        return ResponseEntity.ok().build();
    }
}
