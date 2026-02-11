package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.chat.repository.ChatMessageRepository;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepository {
    private final JpaChatMessageRepository jpaChatMessageRepository;
    private final ChatMessageQuerydslRepository chatMessageQuerydslRepository;

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
    public DomainPage<ChatMessage> findAllByChatRoomId(Long chatRoomId, DomainPageable domainPageable) {
        Pageable pageable = PageRequest.of(
                domainPageable.getPage(),
                domainPageable.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ChatMessageEntity> entityPage = jpaChatMessageRepository.findAllByChatRoomId(chatRoomId, pageable);

        List<ChatMessage> chatMessages = entityPage.getContent().stream()
                .map(ChatMessageEntity::toModel)
                .toList();

        return new DomainPage<>(
                chatMessages,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements()
        );
    }

    @Override
    public List<ChatMessage> findAllByChatRoomId(Long chatRoomId) {
        return jpaChatMessageRepository.findAllByChatRoomIdList(chatRoomId).stream()
                .map(ChatMessageEntity::toModel)
                .toList();
    }

    @Override
    public List<ChatMessage> findChatHistory(Long roomId, Long lastMessageId, int size) {
        return chatMessageQuerydslRepository.findChatHistory(roomId, lastMessageId, size);
    }

    @Override
    public Optional<ChatMessage> findLastMessageByChatRoomId(Long chatRoomId) {
        return jpaChatMessageRepository.findLastMessageByChatRoomId(chatRoomId)
                .map(ChatMessageEntity::toModel);
    }

    @Override
    public long countByChatRoomId(Long chatRoomId) {
        return jpaChatMessageRepository.countByChatRoomId(chatRoomId);
    }

    @Override
    public void delete(ChatMessage chatMessage) {
        ChatMessageEntity entity = ChatMessageEntity.from(chatMessage);
        jpaChatMessageRepository.delete(entity);
    }
}
