package com.back.catchmate.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.BlockCommandService;
import com.back.catchmate.user.application.BlockQueryService;
import com.back.catchmate.user.application.dto.command.BlockCreateCommand;
import com.back.catchmate.user.application.dto.result.BlockCreateResult;
import java.util.List;
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

// UserControllerTest 와 같은 이유로 standalone MockMvc 를 쓴다.
@ExtendWith(MockitoExtension.class)
class BlockControllerTest {

    @Mock
    private BlockCommandService blockCommandService;

    @Mock
    private BlockQueryService blockQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BlockController(blockCommandService, blockQueryService))
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
    @DisplayName("POST /api/blocks 는 201 과 생성된 차단을 돌려준다")
    void createBlock() throws Exception {
        // given
        given(blockCommandService.createBlock(1L, new BlockCreateCommand(2L)))
                .willReturn(new BlockCreateResult(10L, 2L));

        // when & then
        mockMvc.perform(post("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blockedUserId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.blockId").value(10))
                .andExpect(jsonPath("$.blockedUserId").value(2));
    }

    @Test
    @DisplayName("차단 대상이 없으면 400 INVALID_INPUT")
    void createBlockRejectsMissingTarget() throws Exception {
        mockMvc.perform(post("/api/blocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("DELETE /api/blocks/{blockedUserId} 는 204")
    void deleteBlock() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/blocks/2")).andExpect(status().isNoContent());
        then(blockCommandService).should().deleteBlock(1L, 2L);
    }

    @Test
    @DisplayName("GET /api/blocks 의 기본 페이지는 0, 크기는 20 이다")
    void getBlocksDefaults() throws Exception {
        // given
        given(blockQueryService.getBlocks(1L, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(get("/api/blocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void getBlocksRejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/blocks").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
