package com.back.catchmate.board.application.event;

import com.back.catchmate.board.application.BoardCommandService;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BoardEnrollAcceptedListener {
    private final BoardCommandService boardCommandService;

    // 수락과 같은 트랜잭션에서 인원을 늘려야 정원 초과 시 BoardFullException 이 수락까지 롤백하고,
    // @Version 충돌이 EnrollAcceptExecutor 의 재시도로 이어진다. 커밋 후 처리하면 정원을 넘겨 수락된다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(EnrollAcceptedEvent event) {
        boardCommandService.increaseCurrentPerson(event.boardId());
    }
}
