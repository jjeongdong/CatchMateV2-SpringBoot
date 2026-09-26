package com.back.catchmate.chat.domain;

import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;

// 커밋된 메시지를 모든 서버의 구독자에게 퍼뜨린다 (Redis Pub/Sub).
public interface ChatMessageBroadcaster {

    void broadcast(ChatMessageBroadcastEvent event);
}
