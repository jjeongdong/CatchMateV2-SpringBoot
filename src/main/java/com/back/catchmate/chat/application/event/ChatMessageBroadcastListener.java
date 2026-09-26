package com.back.catchmate.chat.application.event;

import com.back.catchmate.chat.domain.ChatMessageBroadcaster;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatMessageBroadcastListener {
    private final ChatMessageBroadcaster chatMessageBroadcaster;

    // 롤백된 메시지를 퍼뜨리지 않도록 커밋 후에, 요청 스레드를 붙잡지 않도록 비동기로 방송한다.
    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ChatMessageBroadcastEvent event) {
        chatMessageBroadcaster.broadcast(event);
    }
}
