package com.back.catchmate.board.adapter.in.event;

import com.back.catchmate.board.application.port.in.BoardInternalCommandUseCase;
import com.back.catchmate.enroll.application.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardEnrollAcceptedEventListener {
    private final BoardInternalCommandUseCase boardInternalCommandUseCase;

    @EventListener
    public void handleEnrollAcceptedEvent(EnrollAcceptedEvent event) {
        boardInternalCommandUseCase.increaseCurrentPerson(event.boardId());
    }
}
