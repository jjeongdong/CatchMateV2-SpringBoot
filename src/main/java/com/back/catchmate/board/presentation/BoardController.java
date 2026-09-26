package com.back.catchmate.board.presentation;

import com.back.catchmate.board.application.BoardCommandService;
import com.back.catchmate.board.application.BoardQueryService;
import com.back.catchmate.board.application.dto.command.BoardSearchCommand;
import com.back.catchmate.board.application.dto.result.BoardCreateResult;
import com.back.catchmate.board.application.dto.result.BoardDetailResult;
import com.back.catchmate.board.application.dto.result.BoardDraftResult;
import com.back.catchmate.board.application.dto.result.BoardLiftUpResult;
import com.back.catchmate.board.application.dto.result.BoardResult;
import com.back.catchmate.board.application.dto.result.BoardUpdateResult;
import com.back.catchmate.board.presentation.dto.request.BoardCreateRequest;
import com.back.catchmate.board.presentation.dto.request.BoardUpdateRequest;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.response.CursorPageResult;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController implements BoardApiDocs {
    private final BoardCommandService boardCommandService;
    private final BoardQueryService boardQueryService;

    @Override
    @PostMapping
    public ResponseEntity<BoardCreateResult> createBoard(
            @AuthUser Long userId, @RequestBody BoardCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardCommandService.createBoard(userId, request.toCommand()));
    }

    @Override
    @GetMapping("/{boardId}")
    public ResponseEntity<BoardDetailResult> getBoard(@AuthUser Long userId, @PathVariable Long boardId) {
        return ResponseEntity.ok(boardQueryService.getBoard(userId, boardId));
    }

    @Override
    @GetMapping("/me/draft")
    public ResponseEntity<BoardDraftResult> getMyDraft(@AuthUser Long userId) {
        return boardQueryService.getMyDraft(userId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent()
                .build());
    }

    @Override
    @GetMapping
    public ResponseEntity<CursorPageResult<BoardResult>> getBoards(
            @AuthUser Long userId,
            @RequestParam(required = false) LocalDate gameDate,
            @RequestParam(required = false) Integer maxPerson,
            @RequestParam(required = false) List<Long> preferredTeamIds,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(boardQueryService.getBoards(
                userId, new BoardSearchCommand(gameDate, maxPerson, preferredTeamIds, cursor, size)));
    }

    @Override
    @GetMapping("/users/{userId}")
    public ResponseEntity<CursorPageResult<BoardResult>> getUserBoards(
            @PathVariable Long userId,
            @AuthUser Long loginUserId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(boardQueryService.getUserBoards(loginUserId, userId, cursor, size));
    }

    @Override
    @PutMapping("/{boardId}")
    public ResponseEntity<BoardUpdateResult> updateBoard(
            @AuthUser Long userId, @PathVariable Long boardId, @RequestBody BoardUpdateRequest request) {
        return ResponseEntity.ok(boardCommandService.updateBoard(userId, boardId, request.toCommand()));
    }

    @Override
    @PostMapping("/{boardId}/lift-up")
    public ResponseEntity<BoardLiftUpResult> liftUpBoard(@AuthUser Long userId, @PathVariable Long boardId) {
        return ResponseEntity.ok(boardCommandService.liftUpBoard(userId, boardId));
    }

    @Override
    @DeleteMapping("/{boardId}")
    public ResponseEntity<Void> deleteBoard(@AuthUser Long userId, @PathVariable Long boardId) {
        boardCommandService.deleteBoard(userId, boardId);
        return ResponseEntity.noContent().build();
    }
}
