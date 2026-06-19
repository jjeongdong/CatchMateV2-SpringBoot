package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.chat.application.event.ChatRoomMemberJoinedEvent;
import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomMemberJoinedEventListener {
    private final ChatInternalCommandUseCase chatInternalCommandUseCase;

    @EventListener
    public void handle(ChatRoomMemberJoinedEvent event) {
        chatInternalCommandUseCase.welcomeNewMember(event.chatRoomId(), event.userId());
    }
}
