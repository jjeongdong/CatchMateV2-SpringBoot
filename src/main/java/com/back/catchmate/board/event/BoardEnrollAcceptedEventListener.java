package com.back.catchmate.board.event;

import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.enroll.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardEnrollAcceptedEventListener {
    private final BoardService boardService;

    @EventListener
    public void handleEnrollAcceptedEvent(EnrollAcceptedEvent event) {
        boardService.increaseCurrentPerson(event.boardId());
    }
}
