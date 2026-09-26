package com.back.catchmate.report.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.report.application.ReportCommandService;
import com.back.catchmate.report.application.ReportQueryService;
import com.back.catchmate.report.application.dto.result.ReportProcessResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// standalone MockMvc 는 보안(@PreAuthorize)을 적용하지 않는다 — 권한은 AdminControllersAuthorizationTest 가 본다.
@ExtendWith(MockitoExtension.class)
class AdminReportControllerTest {

    @Mock
    private ReportCommandService reportCommandService;

    @Mock
    private ReportQueryService reportQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminReportController(reportCommandService, reportQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/reports/{reportId}/process 는 처리 결과를 돌려준다")
    void processReport() throws Exception {
        // given
        given(reportCommandService.processReport(5L)).willReturn(new ReportProcessResult(5L, 2L));

        // when & then
        mockMvc.perform(post("/api/admin/reports/5/process"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportedUserId").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/reports 의 기본 크기는 20 이다")
    void getReportsDefaults() throws Exception {
        // given
        given(reportQueryService.getReports(0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        // when & then
        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void getReportsRejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/admin/reports").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
