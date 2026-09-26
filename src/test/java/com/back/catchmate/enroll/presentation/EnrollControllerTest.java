package com.back.catchmate.enroll.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.enroll.application.EnrollCommandService;
import com.back.catchmate.enroll.application.EnrollQueryService;
import com.back.catchmate.enroll.application.dto.command.EnrollCreateCommand;
import com.back.catchmate.enroll.application.dto.result.EnrollAcceptResult;
import com.back.catchmate.enroll.application.dto.result.EnrollCreateResult;
import com.back.catchmate.enroll.application.dto.result.EnrollPendingCountResult;
import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
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

@ExtendWith(MockitoExtension.class)
class EnrollControllerTest {

    @Mock
    private EnrollCommandService enrollCommandService;

    @Mock
    private EnrollQueryService enrollQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new EnrollController(enrollCommandService, enrollQueryService))
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
    @DisplayName("POST /api/boards/{boardId}/enrolls 는 201")
    void createEnroll() throws Exception {
        given(enrollCommandService.createEnroll(1L, 10L, new EnrollCreateCommand("같이 가요")))
                .willReturn(new EnrollCreateResult(100L, null));

        mockMvc.perform(post("/api/boards/10/enrolls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"같이 가요\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollId").value(100));
    }

    @Test
    @DisplayName("POST /api/enrolls/{enrollId}/accept 는 200")
    void acceptEnroll() throws Exception {
        given(enrollCommandService.acceptEnroll(1L, 100L)).willReturn(EnrollAcceptResult.of(100L));

        mockMvc.perform(post("/api/enrolls/100/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollId").value(100));
    }

    @Test
    @DisplayName("POST /api/enrolls/{enrollId}/read 와 DELETE /api/enrolls/{enrollId} 는 204")
    void readAndDelete() throws Exception {
        mockMvc.perform(post("/api/enrolls/100/read")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/enrolls/100")).andExpect(status().isNoContent());

        then(enrollCommandService).should().markEnrollAsRead(1L, 100L);
        then(enrollCommandService).should().deleteEnroll(1L, 100L);
    }

    @Test
    @DisplayName("GET /api/enrolls/me 와 /api/boards/{boardId}/enrolls 의 기본 크기는 20")
    void listsUseDefaultPaging() throws Exception {
        given(enrollQueryService.getMyEnrolls(1L, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));
        given(enrollQueryService.getBoardEnrolls(1L, 10L, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/enrolls/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
        mockMvc.perform(get("/api/boards/10/enrolls")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/enrolls/received/pending-count 는 대기 건수를 돌려준다")
    void getPendingEnrollCount() throws Exception {
        given(enrollQueryService.getPendingEnrollCount(1L)).willReturn(new EnrollPendingCountResult(3));

        mockMvc.perform(get("/api/enrolls/received/pending-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/enrolls/received").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
