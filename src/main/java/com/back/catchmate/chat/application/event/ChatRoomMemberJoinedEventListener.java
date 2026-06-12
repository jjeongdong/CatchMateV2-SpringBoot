package com.back.catchmate.chat.application.event;

import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.chat.domain.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberJoinedEventListener {
    private final ChatRoomService chatRoomService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 메인 트랜잭션 내에서 입장 시스템 메시지를 저장하고,
     * 브로드캐스트용 {@link ChatMessageEvent} 를 후속 발행한다.
     */
    @EventListener
    public void handle(ChatRoomMemberJoinedEvent event) {
        ChatMessage joinMessage = chatRoomService.enterChatRoom(event.chatRoomId(), event.user());
        applicationEventPublisher.publishEvent(ChatMessageEvent.from(joinMessage, event.user()));
    }
}
