package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import com.back.catchmate.enroll.application.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEnrollAcceptedEventListener {
    private final ChatInternalCommandUseCase chatInternalCommandUseCase;

    @EventListener
    public void handle(EnrollAcceptedEvent event) {
        chatInternalCommandUseCase.addBoardChatRoomMember(event.boardId(), event.applicantId());
    }
}
