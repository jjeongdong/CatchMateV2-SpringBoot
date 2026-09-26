package com.back.catchmate.notice.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.NoticeCommandService;
import com.back.catchmate.notice.application.NoticeQueryService;
import com.back.catchmate.notice.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.notice.application.dto.result.NoticeCreateResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import java.time.LocalDateTime;
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

// standalone MockMvc 는 보안(@PreAuthorize)을 적용하지 않는다 — 권한은 AdminControllersAuthorizationTest 가 본다.
@ExtendWith(MockitoExtension.class)
class AdminNoticeControllerTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    @Mock
    private NoticeCommandService noticeCommandService;

    @Mock
    private NoticeQueryService noticeQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminNoticeController(noticeCommandService, noticeQueryService))
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
    @DisplayName("POST /api/admin/notices 는 201 과 생성 결과를 돌려준다")
    void createNotice() throws Exception {
        // given
        given(noticeCommandService.createNotice(1L, new NoticeCreateCommand("제목", "내용")))
                .willReturn(new NoticeCreateResult(10L, CREATED_AT));

        // when & then
        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.noticeId").value(10));
    }

    @Test
    @DisplayName("제목이 비어 있으면 400 INVALID_INPUT")
    void createNoticeRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \",\"content\":\"내용\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("PUT /api/admin/notices/{noticeId} 는 수정된 상세를 돌려준다")
    void updateNotice() throws Exception {
        // given
        given(noticeCommandService.updateNotice(10L, new NoticeUpdateCommand("새 제목", "새 내용")))
                .willReturn(new NoticeDetailResult(10L, "새 제목", "새 내용", "관리자", CREATED_AT));

        // when & then
        mockMvc.perform(put("/api/admin/notices/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"새 제목\",\"content\":\"새 내용\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("새 제목"));
    }

    @Test
    @DisplayName("DELETE /api/admin/notices/{noticeId} 는 204")
    void deleteNotice() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/admin/notices/10")).andExpect(status().isNoContent());
        then(noticeCommandService).should().deleteNotice(10L);
    }

    @Test
    @DisplayName("GET /api/admin/notices 의 기본 페이지는 0, 크기는 20 이다")
    void getNoticesDefaults() throws Exception {
        // given
        given(noticeQueryService.getNotices(0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(get("/api/admin/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void getNoticesRejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/admin/notices").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
