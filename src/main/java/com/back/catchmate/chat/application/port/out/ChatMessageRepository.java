package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.chat.domain.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    void saveAll(List<ChatMessage> chatMessages);

    void delete(ChatMessage chatMessage);
}
