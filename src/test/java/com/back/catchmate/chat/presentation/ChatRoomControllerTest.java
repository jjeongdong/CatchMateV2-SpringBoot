package com.back.catchmate.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.chat.application.ChatCommandService;
import com.back.catchmate.chat.application.ChatQueryService;
import com.back.catchmate.chat.application.dto.result.ChatMessageResult;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import java.util.List;
import java.util.Optional;
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
class ChatRoomControllerTest {

    private static final ChatMessageResult MESSAGE =
            new ChatMessageResult(100L, 5L, 2L, "철수", null, "안녕", MessageType.TEXT, null);

    @Mock
    private ChatCommandService chatCommandService;

    @Mock
    private ChatQueryService chatQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ChatRoomController(chatCommandService, chatQueryService))
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
    @DisplayName("GET /api/chat-rooms 는 내 채팅방 목록을 오프셋 페이지로 돌려준다")
    void getMyChatRooms() throws Exception {
        // given
        given(chatQueryService.getMyChatRooms(1L, 1, 10)).willReturn(OffsetPageResult.of(List.of(), 1, 10, 11));

        // when & then
        mockMvc.perform(get("/api/chat-rooms").param("page", "1").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(11));
    }

    @Test
    @DisplayName("GET /api/chat-rooms/{id}/messages 는 cursor 를 넘기고 커서 페이지를 돌려준다")
    void getMessages() throws Exception {
        // given
        given(chatQueryService.getMessages(1L, 5L, "120", 20))
                .willReturn(new CursorPageResult<>(List.of(MESSAGE), "100", true));

        // when & then
        mockMvc.perform(get("/api/chat-rooms/5/messages").param("cursor", "120"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value(100))
                .andExpect(jsonPath("$.content[0].senderNickName").value("철수"))
                .andExpect(jsonPath("$.nextCursor").value("100"))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    @DisplayName("size 가 100 을 넘으면 400 INVALID_INPUT")
    void rejectsTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/chat-rooms/5/messages").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("GET /api/chat-rooms/{id}/messages/sync 는 size 기본값 100 으로 동기화한다")
    void syncMessages() throws Exception {
        // given
        given(chatQueryService.syncMessages(1L, 5L, 90L, 100)).willReturn(CursorPageResult.empty());

        // when & then
        mockMvc.perform(get("/api/chat-rooms/5/messages/sync").param("lastMessageId", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @DisplayName("마지막 메시지가 없으면 204, 있으면 200")
    void getLastMessage() throws Exception {
        // given
        given(chatQueryService.getLastMessage(1L, 5L)).willReturn(Optional.empty());
        given(chatQueryService.getLastMessage(1L, 6L)).willReturn(Optional.of(MESSAGE));

        // when & then
        mockMvc.perform(get("/api/chat-rooms/5/messages/last")).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/chat-rooms/6/messages/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("안녕"));
    }

    @Test
    @DisplayName("GET /api/chat-rooms/{id}/members 는 참여자 목록을 돌려준다")
    void getChatRoomMembers() throws Exception {
        // given
        given(chatQueryService.getChatRoomMembers(1L, 5L)).willReturn(List.of());

        // when & then
        mockMvc.perform(get("/api/chat-rooms/5/members")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/chat-rooms/{id}/notifications 는 notificationOn 키로 받아 204")
    void updateNotificationSetting() throws Exception {
        mockMvc.perform(put("/api/chat-rooms/5/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notificationOn\":false}"))
                .andExpect(status().isNoContent());

        then(chatCommandService).should().updateNotificationSetting(1L, 5L, false);
    }

    @Test
    @DisplayName("알림 설정 body 에 notificationOn 이 없으면 400 INVALID_INPUT")
    void updateNotificationSettingRejectsMissingField() throws Exception {
        mockMvc.perform(put("/api/chat-rooms/5/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("PATCH /api/chat-rooms/{id}/image 는 chatRoomImage 파트를 UploadFile 로 바꿔 넘기고 204")
    void updateChatRoomImage() throws Exception {
        // given
        MockMultipartFile image = new MockMultipartFile("chatRoomImage", "a.png", "image/png", new byte[] {1, 2});

        // when
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/chat-rooms/5/image").file(image))
                .andExpect(status().isNoContent());

        // then
        ArgumentCaptor<UploadFile> file = ArgumentCaptor.forClass(UploadFile.class);
        then(chatCommandService).should().updateChatRoomImage(eq(1L), eq(5L), file.capture());
        assertThat(file.getValue().originalFilename()).isEqualTo("a.png");
        assertThat(file.getValue().size()).isEqualTo(2L);
    }

    @Test
    @DisplayName("DELETE /api/chat-rooms/{id}/members/me 는 퇴장이고 강퇴를 부르지 않는다")
    void leaveChatRoom() throws Exception {
        mockMvc.perform(delete("/api/chat-rooms/5/members/me")).andExpect(status().isNoContent());

        then(chatCommandService).should().leaveChatRoom(1L, 5L);
        then(chatCommandService).should(never()).kickChatRoomMember(any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/chat-rooms/{id}/members/{userId} 는 강퇴다")
    void kickChatRoomMember() throws Exception {
        mockMvc.perform(delete("/api/chat-rooms/5/members/7")).andExpect(status().isNoContent());

        then(chatCommandService).should().kickChatRoomMember(1L, 5L, 7L);
    }
}
