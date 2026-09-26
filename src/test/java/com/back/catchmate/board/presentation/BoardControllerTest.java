package com.back.catchmate.board.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.board.application.BoardCommandService;
import com.back.catchmate.board.application.BoardQueryService;
import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardSearchCommand;
import com.back.catchmate.board.application.dto.result.BoardCreateResult;
import com.back.catchmate.board.application.dto.result.BoardLiftUpResult;
import com.back.catchmate.board.domain.exception.BoardCursorInvalidException;
import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.CursorPageResult;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// 보안 필터 구성을 피하려고 standalone MockMvc 를 쓴다 (user 컨트롤러 테스트와 같은 방식).
@ExtendWith(MockitoExtension.class)
class BoardControllerTest {

    @Mock
    private BoardCommandService boardCommandService;

    @Mock
    private BoardQueryService boardQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BoardController(boardCommandService, boardQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthUserArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST /api/boards 는 201 과 생성된 게시글 ID 를 돌려준다")
    void createBoard() throws Exception {
        // given
        given(boardCommandService.createBoard(1L, new BoardCreateCommand("t", null, 0, null, null, null, null, false)))
                .willReturn(new BoardCreateResult(10L, null));

        // when & then
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"completed\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.boardId").value(10));
    }

    @Test
    @DisplayName("발행 여부가 없으면 400 INVALID_INPUT")
    void createBoardRequiresCompleted() throws Exception {
        mockMvc.perform(post("/api/boards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("임시저장 글이 없으면 GET /api/boards/me/draft 는 204")
    void getMyDraftEmpty() throws Exception {
        given(boardQueryService.getMyDraft(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/api/boards/me/draft")).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/boards 의 기본 크기는 20 이고 커서 없이 첫 페이지를 읽는다")
    void getBoardsDefaults() throws Exception {
        // given
        given(boardQueryService.getBoards(1L, new BoardSearchCommand(null, null, null, null, 20)))
                .willReturn(CursorPageResult.empty());

        // when & then
        mockMvc.perform(get("/api/boards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("손상된 커서는 400 BOARD_CURSOR_INVALID")
    void rejectsBrokenCursor() throws Exception {
        // given
        given(boardQueryService.getBoards(eq(1L), any(BoardSearchCommand.class)))
                .willThrow(new BoardCursorInvalidException());

        // when & then
        mockMvc.perform(get("/api/boards").param("cursor", "broken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOARD_CURSOR_INVALID"));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/boards").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("POST /api/boards/{boardId}/lift-up 은 끌어올리기 결과를 돌려준다")
    void liftUpBoard() throws Exception {
        given(boardCommandService.liftUpBoard(1L, 10L)).willReturn(BoardLiftUpResult.lifted());

        mockMvc.perform(post("/api/boards/10/lift-up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liftedUp").value(true));
    }

    @Test
    @DisplayName("DELETE /api/boards/{boardId} 는 204")
    void deleteBoard() throws Exception {
        mockMvc.perform(delete("/api/boards/10")).andExpect(status().isNoContent());

        then(boardCommandService).should().deleteBoard(1L, 10L);
    }
}
