package com.back.catchmate.board.presentation;

import com.back.catchmate.board.application.BoardQueryService;
import com.back.catchmate.board.application.dto.result.AdminBoardDetailResult;
import com.back.catchmate.board.application.dto.result.AdminBoardResult;
import com.back.catchmate.global.response.OffsetPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// SecurityConfig 에 /api/admin/** 규칙이 없어 이 어노테이션이 관리자 보호의 전부다.
@RestController
@RequestMapping("/api/admin/boards")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminBoardController implements AdminBoardApiDocs {
    private final BoardQueryService boardQueryService;

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<AdminBoardResult>> getAdminBoards(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(boardQueryService.getAdminBoards(userId, page, size));
    }

    @Override
    @GetMapping("/{boardId}")
    public ResponseEntity<AdminBoardDetailResult> getAdminBoard(@PathVariable Long boardId) {
        return ResponseEntity.ok(boardQueryService.getAdminBoard(boardId));
    }
}
