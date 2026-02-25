package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.repository.ChatMessageRepository;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final JpaChatMessageRepository jpaChatMessageRepository;
    private final QueryDSLChatMessageRepository querydslChatMessageRepository;

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
    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        return querydslChatMessageRepository.findChatHistory(roomId, lastMessageId, size);
    }

    @Override
    public List<ChatMessage> findSyncMessages(Long roomId, Long lastMessageId, int size) {
        return querydslChatMessageRepository.findSyncMessages(roomId, lastMessageId, size);
    }

    @Override
    public void delete(ChatMessage chatMessage) {
        ChatMessageEntity entity = ChatMessageEntity.from(chatMessage);
        jpaChatMessageRepository.delete(entity);
    }
}
