package com.back.catchmate.chat.adapter.out.persistence.repository;

import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.application.port.out.ChatMessageRepository;
import com.back.catchmate.chat.adapter.out.persistence.entity.ChatMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final JpaChatMessageRepository jpaChatMessageRepository;
    private final QueryDslChatMessageRepository queryDslChatMessageRepository;
    private final JdbcChatMessageBatchWriter jdbcChatMessageBatchWriter;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        ChatMessageEntity entity = ChatMessageEntity.from(chatMessage);
        return jpaChatMessageRepository.save(entity).toModel();
    }

    @Override
    public Optional<ChatMessage> findById(Long id) {
        return jpaChatMessageRepository.findById(id)
                .map(ChatMessageEntity::toModel);
    }

    @Override
    public Optional<ChatMessage> findLastTextMessageByChatRoomId(Long chatRoomId) {
        return jpaChatMessageRepository.findTopByChatRoomIdAndMessageTypeOrderByIdDesc(chatRoomId, MessageType.TEXT)
                .map(ChatMessageEntity::toModel);
    }

    @Override
    public Map<Long, ChatMessage> findLastTextMessagesByChatRoomIds(List<Long> chatRoomIds) {
        return queryDslChatMessageRepository.findLastTextMessagesByChatRoomIds(chatRoomIds);
    }

    @Override
    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        return queryDslChatMessageRepository.findChatHistory(roomId, lastMessageId, size);
    }

    @Override
    public List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size) {
        return queryDslChatMessageRepository.findSyncMessages(roomId, lastMessageId, size);
    }

    @Override
    public void saveAll(List<ChatMessage> chatMessages) {
        jdbcChatMessageBatchWriter.batchInsert(chatMessages);
    }

    @Override
    public void delete(ChatMessage chatMessage) {
        ChatMessageEntity entity = ChatMessageEntity.from(chatMessage);
        jpaChatMessageRepository.delete(entity);
    }
}
