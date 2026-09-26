package com.back.catchmate.admin.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.admin.application.AdminQueryService;
import com.back.catchmate.admin.application.dto.result.AdminDashboardResult;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// standalone MockMvc 는 @PreAuthorize 를 적용하지 않는다 — 권한은 AdminControllersAuthorizationTest 가 본다.
@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    @Mock
    private AdminQueryService adminQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminDashboardController(adminQueryService))
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/dashboard/stats 는 옛 응답과 같은 키로 통계를 돌려준다")
    void getDashboard() throws Exception {
        given(adminQueryService.getDashboard())
                .willReturn(new AdminDashboardResult(
                        10L, new AdminDashboardResult.GenderRatio(6L, 4L), 20L, Map.of(), Map.of(), 3L, 1L, 5L, 2L));

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUserCount").value(10))
                .andExpect(jsonPath("$.genderRatio.maleCount").value(6))
                .andExpect(jsonPath("$.waitingInquiryCount").value(2));
    }
}
