package com.back.catchmate.chat.presentation;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import com.back.catchmate.chat.application.ChatCommandService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@ExtendWith(MockitoExtension.class)
class ChatSessionEventHandlerTest {

    @Mock
    private ChatCommandService chatCommandService;

    @InjectMocks
    private ChatSessionEventHandler sut;

    private static Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setUser(new UsernamePasswordAuthenticationToken("1", null, List.of()));
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("채팅방을 구독하면 읽음 처리하고 그 방을 포커스한다")
    void subscribeFocusesRoom() {
        sut.handleSubscribe(new SessionSubscribeEvent(this, frame(StompCommand.SUBSCRIBE, "/sub/chat/room/5")));

        then(chatCommandService).should().readChatRoom(1L, 5L);
        then(chatCommandService).should().focusChatRoom(1L, 5L);
    }

    @Test
    @DisplayName("채팅방이 아닌 구독은 무시한다")
    void ignoresOtherSubscription() {
        sut.handleSubscribe(new SessionSubscribeEvent(this, frame(StompCommand.SUBSCRIBE, "/user/queue/errors")));

        then(chatCommandService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("연결·구독 취소 때 이전 세션이 남긴 포커스를 지운다")
    void connectAndUnsubscribeClearFocus() {
        sut.handleConnect(new SessionConnectedEvent(this, frame(StompCommand.CONNECTED, null)));
        sut.handleUnsubscribe(new SessionUnsubscribeEvent(this, frame(StompCommand.UNSUBSCRIBE, null)));

        then(chatCommandService).should(times(2)).unfocusChatRoom(1L);
    }
}
