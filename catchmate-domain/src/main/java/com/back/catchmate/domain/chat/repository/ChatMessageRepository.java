package com.back.catchmate.domain.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findById(Long id);

    Optional<ChatMessage> findLastTextMessageByChatRoomId(Long chatRoomId);

    /**
     * 여러 채팅방의 마지막 TEXT 메시지를 한 번에 조회
     * @return chatRoomId → ChatMessage 맵
     */
    Map<Long, ChatMessage> findLastTextMessagesByChatRoomIds(List<Long> chatRoomIds);

    List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size);

    List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size);

    void delete(ChatMessage chatMessage);
}
