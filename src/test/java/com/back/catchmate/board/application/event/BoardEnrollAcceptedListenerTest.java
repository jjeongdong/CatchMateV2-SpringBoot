package com.back.catchmate.board.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.board.application.BoardCommandService;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class BoardEnrollAcceptedListenerTest {

    @Mock
    private BoardCommandService boardCommandService;

    @InjectMocks
    private BoardEnrollAcceptedListener listener;

    @Test
    @DisplayName("수락과 같은 트랜잭션(BEFORE_COMMIT)에서 인원을 늘린다 — 커밋 후면 정원 초과가 롤백되지 않는다")
    void runsBeforeCommit() throws Exception {
        // when
        TransactionalEventListener annotation = BoardEnrollAcceptedListener.class
                .getMethod("handle", EnrollAcceptedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        // then
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    }

    @Test
    @DisplayName("수락된 게시글의 인원을 늘린다")
    void increasesCurrentPerson() {
        // when
        listener.handle(new EnrollAcceptedEvent(55L, 10L, 3L, 2L));

        // then
        then(boardCommandService).should().increaseCurrentPerson(10L);
    }
}
