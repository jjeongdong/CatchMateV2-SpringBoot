package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomMemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepository {
    private final JpaChatRoomMemberRepository jpaChatRoomMemberRepository;

    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        ChatRoomMemberEntity entity = ChatRoomMemberEntity.from(member);
        return jpaChatRoomMemberRepository.save(entity).toModel();
    }

    @Override
    public Optional<ChatRoomMember> findById(Long id) {
        return jpaChatRoomMemberRepository.findById(id)
                .map(ChatRoomMemberEntity::toModel);
    }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return jpaChatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(ChatRoomMemberEntity::toModel);
    }

    @Override
    public List<ChatRoomMember> findAllByUserIdAndActive(Long userId) {
        return jpaChatRoomMemberRepository.findAllByUserIdAndActive(userId).stream()
                .map(ChatRoomMemberEntity::toModel)
                .toList();
    }

    @Override
    public List<ChatRoomMember> findAllByChatRoomIdAndActive(Long chatRoomId) {
        return jpaChatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId).stream()
                .map(ChatRoomMemberEntity::toModel)
                .toList();
    }

    @Override
    public boolean existsByChatRoomIdAndUserIdAndActive(Long chatRoomId, Long userId) {
        return jpaChatRoomMemberRepository.existsByChatRoomIdAndUserIdAndActive(chatRoomId, userId);
    }

    @Override
    public void delete(ChatRoomMember member) {
        ChatRoomMemberEntity entity = ChatRoomMemberEntity.from(member);
        jpaChatRoomMemberRepository.delete(entity);
    }

    @Override
    public void updateLastReadSequenceDirectly(Long chatRoomId, Long userId, Long sequence) {
        jpaChatRoomMemberRepository.updateLastReadSequenceDirectly(chatRoomId, userId, sequence);
    }
}
