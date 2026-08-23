package com.back.catchmate.chat.event;

import com.back.catchmate.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberJoinedEventListener {
    private final ChatCommandService chatCommandService;

    @EventListener
    public void handle(ChatRoomMemberJoinedEvent event) {
        chatCommandService.welcomeNewMember(event.chatRoomId(), event.userId());
    }
}
