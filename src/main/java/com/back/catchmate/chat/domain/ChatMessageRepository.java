package com.back.catchmate.chat.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatMessageRepository {

    ChatMessage save(ChatMessage message);

    // beforeMessageId 보다 이전(null 이면 최신) 메시지 limit 개를 오래된 순으로
    List<ChatMessage> findHistory(Long chatRoomId, Long beforeMessageId, int limit);

    // afterMessageId 보다 이후(null 이면 처음) 메시지 limit 개를 오래된 순으로
    List<ChatMessage> findAfter(Long chatRoomId, Long afterMessageId, int limit);

    Optional<ChatMessage> findLastText(Long chatRoomId);

    // 방 ID → 그 방의 마지막 TEXT 메시지. TEXT 가 없는 방은 빠진다.
    Map<Long, ChatMessage> findLastTextByChatRoomIds(Collection<Long> chatRoomIds);
}
