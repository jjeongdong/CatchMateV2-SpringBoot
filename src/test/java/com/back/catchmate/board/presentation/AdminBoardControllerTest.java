package com.back.catchmate.board.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.board.application.BoardQueryService;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// 권한(@PreAuthorize)은 AdminControllersAuthorizationTest 가 확인한다. 여기서는 요청 매핑만 본다.
@ExtendWith(MockitoExtension.class)
class AdminBoardControllerTest {

    @Mock
    private BoardQueryService boardQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminBoardController(boardQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/boards?userId= 는 그 작성자의 게시글을 기본 크기 20 으로 조회한다")
    void getAdminBoardsByWriter() throws Exception {
        // given
        given(boardQueryService.getAdminBoards(2L, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(get("/api/admin/boards").param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }
}
