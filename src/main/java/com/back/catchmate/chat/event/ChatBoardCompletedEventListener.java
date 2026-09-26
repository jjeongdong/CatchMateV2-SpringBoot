package com.back.catchmate.chat.event;

import com.back.catchmate.board.domain.event.BoardCompletedEvent;
import com.back.catchmate.chat.service.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatBoardCompletedEventListener {
    private final ChatCommandService chatCommandService;

    @EventListener
    public void handle(BoardCompletedEvent event) {
        chatCommandService.addBoardChatRoomMember(event.boardId(), event.ownerId());
    }
}
