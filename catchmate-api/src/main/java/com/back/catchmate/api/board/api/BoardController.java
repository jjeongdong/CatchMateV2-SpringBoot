package com.back.catchmate.api.board.api;

import com.back.catchmate.api.board.dto.request.BoardCreateRequest;
import com.back.catchmate.application.board.BoardUseCase;
import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "[사용자] 게시글 관련 API")
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {
    private final BoardUseCase boardUseCase;

    @PostMapping
    @Operation(summary = "게시글 생성/임시저장 API", description = "게시글을 생성하거나 임시저장합니다. (isCompleted: true=게시, false=임시저장)")
    public ResponseEntity<BoardResponse> writeBoard(@AuthUser Long userId,
                                                    @Valid @RequestBody BoardCreateRequest request) {
        return ResponseEntity.ok(boardUseCase.writeBoard(userId, request.toCommand()));
    }
}
