package com.back.catchmate.chat.event;

import com.back.catchmate.chat.service.ChatCommandService;
import com.back.catchmate.enroll.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEnrollAcceptedEventListener {
    private final ChatCommandService chatCommandService;

    @EventListener
    public void handle(EnrollAcceptedEvent event) {
        chatCommandService.addBoardChatRoomMember(event.boardId(), event.applicantId());
    }
}
