package com.back.catchmate.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.user.application.UserCommandService;
import com.back.catchmate.user.application.UserQueryService;
import com.back.catchmate.user.application.dto.command.UserAlarmUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserFcmTokenUpdateCommand;
import com.back.catchmate.user.application.dto.command.UserProfileUpdateCommand;
import com.back.catchmate.user.application.dto.result.NicknameAvailabilityResult;
import com.back.catchmate.user.application.dto.result.UserAlarmResult;
import com.back.catchmate.user.application.dto.result.UserResult;
import com.back.catchmate.user.domain.UserAlarmType;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

// JWT 필터 구성을 피하려고 standalone MockMvc 를 쓰고, @AuthUser 는 실제 리졸버에 SecurityContext 를 채워 해석한다.
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final UserResult USER_RESULT = new UserResult(
            1L, "홍길동", "a@catchmate.com", "url", 'M', LocalDate.of(2000, 1, 1), "응원형", null, "ROLE_USER");

    @Mock
    private UserCommandService userCommandService;

    @Mock
    private UserQueryService userQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userCommandService, userQueryService))
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
    @DisplayName("GET /api/users/me 는 로그인 유저의 프로필을 돌려준다")
    void getMyProfile() throws Exception {
        // given
        given(userQueryService.getMyProfile(1L)).willReturn(USER_RESULT);

        // when & then
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.nickName").value("홍길동"));
    }

    @Test
    @DisplayName("GET /api/users/{userId} 는 다른 유저의 프로필을 돌려준다")
    void getUser() throws Exception {
        // given
        given(userQueryService.getUser(7L)).willReturn(USER_RESULT);

        // when & then
        mockMvc.perform(get("/api/users/7")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/users/nickname-availability 는 사용 가능 여부를 available 키로 돌려준다")
    void getNicknameAvailability() throws Exception {
        // given
        given(userQueryService.getNicknameAvailability("홍길동")).willReturn(new NicknameAvailabilityResult("홍길동", true));

        // when & then
        mockMvc.perform(get("/api/users/nickname-availability").param("nickName", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("PATCH /api/users/me/alarms 는 body 로 받아 바뀐 전체 알림 상태를 돌려준다")
    void updateMyAlarm() throws Exception {
        // given
        given(userCommandService.updateMyAlarm(1L, new UserAlarmUpdateCommand(UserAlarmType.CHAT, false)))
                .willReturn(new UserAlarmResult(true, false, true, true));

        // when & then
        mockMvc.perform(patch("/api/users/me/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmType\":\"CHAT\",\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatAlarm").value(false));
    }

    @Test
    @DisplayName("알림 설정 body 에 enabled 가 없으면 400 INVALID_INPUT")
    void updateMyAlarmRejectsMissingField() throws Exception {
        mockMvc.perform(patch("/api/users/me/alarms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"alarmType\":\"CHAT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/users/me/alarms 는 알림 설정을 돌려준다")
    void getMyAlarms() throws Exception {
        // given
        given(userQueryService.getMyAlarms(1L)).willReturn(new UserAlarmResult(true, true, true, true));

        // when & then
        mockMvc.perform(get("/api/users/me/alarms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allAlarm").value(true));
    }

    @Test
    @DisplayName("PUT /api/users/me/fcm-token 은 204")
    void updateFcmToken() throws Exception {
        // when & then
        mockMvc.perform(put("/api/users/me/fcm-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fcmToken\":\"token\"}"))
                .andExpect(status().isNoContent());
        then(userCommandService).should().updateFcmToken(new UserFcmTokenUpdateCommand(1L, "token"));
    }

    @Test
    @DisplayName("PATCH /api/users/me 는 multipart 의 request 와 이미지를 넘긴다")
    void updateMyProfile() throws Exception {
        // given
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"nickName\":\"새닉네임\"}".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile image =
                new MockMultipartFile("profileImage", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[] {1, 2});
        given(userCommandService.updateMyProfile(eq(1L), eq(new UserProfileUpdateCommand("새닉네임", null, null)), any()))
                .willReturn(USER_RESULT);

        // when
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .file(request)
                        .file(image))
                .andExpect(status().isOk());

        // then
        ArgumentCaptor<UploadFile> captor = ArgumentCaptor.forClass(UploadFile.class);
        then(userCommandService).should().updateMyProfile(eq(1L), any(), captor.capture());
        assertThat(captor.getValue().originalFilename()).isEqualTo("a.png");
        assertThat(captor.getValue().size()).isEqualTo(2);
    }

    @Test
    @DisplayName("닉네임이 2자 미만이면 400 INVALID_INPUT")
    void updateMyProfileRejectsShortNickName() throws Exception {
        // given
        MockMultipartFile request = new MockMultipartFile(
                "request",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                "{\"nickName\":\"a\"}".getBytes(StandardCharsets.UTF_8));

        // when & then
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").file(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
