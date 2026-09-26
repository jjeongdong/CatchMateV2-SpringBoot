package com.back.catchmate.chat.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.board.domain.event.BoardCompletedEvent;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class ChatListenersTest {

    @Test
    @DisplayName("게시글 발행·신청 수락의 채팅방 입장은 같은 트랜잭션(동기)에서 처리한다 — 입장 실패가 원래 작업을 취소해야 한다")
    void runsSynchronously() throws Exception {
        Method completed = ChatBoardCompletedListener.class.getMethod("handle", BoardCompletedEvent.class);
        Method accepted = ChatEnrollAcceptedListener.class.getMethod("handle", EnrollAcceptedEvent.class);

        assertThat(completed.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(completed.isAnnotationPresent(TransactionalEventListener.class))
                .isFalse();
        assertThat(accepted.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(accepted.isAnnotationPresent(TransactionalEventListener.class))
                .isFalse();
    }

    @Test
    @DisplayName("방송은 커밋 후 비동기로 한다 — 롤백된 메시지를 퍼뜨리지 않고 요청 스레드를 붙잡지 않게")
    void broadcastsAfterCommitAsync() throws Exception {
        Method broadcast = ChatMessageBroadcastListener.class.getMethod("handle", ChatMessageBroadcastEvent.class);

        assertThat(broadcast.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.AFTER_COMMIT);
        assertThat(broadcast.getAnnotation(Async.class).value()).isEqualTo("taskExecutor");
    }
}
