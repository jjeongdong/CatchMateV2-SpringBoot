package com.back.catchmate.domain.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {
    ChatMessage save(ChatMessage chatMessage);

    Optional<ChatMessage> findById(Long id);

    DomainPage<ChatMessage> findAllByChatRoomId(Long chatRoomId, DomainPageable pageable);

    List<ChatMessage> findAllByChatRoomId(Long chatRoomId);

    List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size);

    Optional<ChatMessage> findLastMessageByChatRoomId(Long chatRoomId);

    long countByChatRoomId(Long chatRoomId);

    void delete(ChatMessage chatMessage);
}
