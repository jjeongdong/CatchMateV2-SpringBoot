package com.back.catchmate.notification.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.notification.application.NotificationCommandService;
import com.back.catchmate.notification.application.NotificationQueryService;
import com.back.catchmate.notification.application.dto.result.NotificationReadAllResult;
import com.back.catchmate.notification.application.dto.result.NotificationResult;
import com.back.catchmate.notification.application.dto.result.NotificationUnreadResult;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// JWT 필터 구성을 피하려고 standalone MockMvc 를 쓰고, @AuthUser 는 실제 리졸버에 SecurityContext 를 채워 해석한다.
@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    private static final NotificationResult RESULT =
            new NotificationResult(3L, "알림", "ENROLL", false, null, null, "철수", 100L, 10L, "", "ACCEPTED");

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private NotificationQueryService notificationQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new NotificationController(notificationCommandService, notificationQueryService))
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
    @DisplayName("GET /api/notifications 는 cursor 를 넘기고 커서 페이지를 돌려준다 (기본 size 20)")
    void getNotifications() throws Exception {
        given(notificationQueryService.getNotifications(1L, "abc", 20))
                .willReturn(new CursorPageResult<>(List.of(RESULT), "next", true));

        mockMvc.perform(get("/api/notifications").param("cursor", "abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(3))
                .andExpect(jsonPath("$.content[0].acceptStatus").value("ACCEPTED"))
                .andExpect(jsonPath("$.nextCursor").value("next"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/notifications").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/notifications/{id} 는 상세를 돌려준다")
    void getNotification() throws Exception {
        given(notificationQueryService.getNotification(1L, 3L)).willReturn(RESULT);

        mockMvc.perform(get("/api/notifications/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderNickname").value("철수"));
    }

    @Test
    @DisplayName("POST /api/notifications/{id}/read 는 204, 옛 PATCH 는 405")
    void markAsRead() throws Exception {
        mockMvc.perform(post("/api/notifications/3/read")).andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/notifications/3/read")).andExpect(status().isMethodNotAllowed());

        then(notificationCommandService).should().markNotificationAsRead(1L, 3L);
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} 는 204")
    void deleteNotification() throws Exception {
        mockMvc.perform(delete("/api/notifications/3")).andExpect(status().isNoContent());

        then(notificationCommandService).should().deleteNotification(1L, 3L);
    }

    @Test
    @DisplayName("GET /api/notifications/unread 는 상세 조회로 잡히지 않고 안 읽음 여부를 돌려준다")
    void getUnread() throws Exception {
        given(notificationQueryService.getUnread(1L)).willReturn(new NotificationUnreadResult(true));

        mockMvc.perform(get("/api/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasUnread").value(true));
    }

    @Test
    @DisplayName("POST /api/notifications/read-all 은 갱신 건수를 돌려준다")
    void readAll() throws Exception {
        given(notificationCommandService.readAllNotifications(1L)).willReturn(new NotificationReadAllResult(4));

        mockMvc.perform(post("/api/notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(4));
    }
}
