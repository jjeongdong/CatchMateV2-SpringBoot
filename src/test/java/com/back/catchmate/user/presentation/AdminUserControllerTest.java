package com.back.catchmate.user.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryService;
import com.back.catchmate.user.application.dto.result.AdminUserDetailResult;
import java.util.List;
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
class AdminUserControllerTest {

    @Mock
    private UserQueryService userQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(userQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/users/{userId} 는 회원 상세를 돌려준다")
    void getAdminUser() throws Exception {
        given(userQueryService.getAdminUser(1L))
                .willReturn(new AdminUserDetailResult(
                        1L, "a@catchmate.com", "홍길동", "KAKAO", 'M', null, null, null, "ROLE_USER", false, null, null));

        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickName").value("홍길동"));
    }

    @Test
    @DisplayName("GET /api/admin/users 의 기본 페이지는 0, 크기는 20 이다")
    void getAdminUsersDefaults() throws Exception {
        given(userQueryService.getAdminUsers(null, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/admin/users").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
