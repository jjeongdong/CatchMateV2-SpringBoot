package com.back.catchmate.chat.adapter.in.event;

import com.back.catchmate.board.application.event.BoardCompletedEvent;
import com.back.catchmate.chat.application.port.in.ChatInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatBoardCompletedEventListener {
    private final ChatInternalCommandUseCase chatInternalCommandUseCase;

    @EventListener
    public void handle(BoardCompletedEvent event) {
        chatInternalCommandUseCase.addBoardChatRoomMember(event.boardId(), event.ownerId());
    }
}
