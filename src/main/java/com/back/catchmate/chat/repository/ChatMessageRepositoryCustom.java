package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.entity.ChatMessage;

import java.util.List;
import java.util.Map;

public interface ChatMessageRepositoryCustom {
    List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size);

    List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size);

    /**
     * 여러 채팅방의 마지막 TEXT 메시지를 한 번에 조회
     */
    Map<Long, ChatMessage> findLastTextMessagesByChatRoomIds(List<Long> chatRoomIds);
}
