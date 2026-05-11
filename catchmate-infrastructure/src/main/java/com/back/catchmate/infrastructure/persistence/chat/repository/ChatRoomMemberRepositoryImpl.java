package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatRoomMember;
import com.back.catchmate.domain.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.infrastructure.persistence.chat.entity.ChatRoomMemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepository {
    private final JpaChatRoomMemberRepository jpaChatRoomMemberRepository;
    private final JdbcTemplate jdbcTemplate;

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
    public Map<Long, ChatRoomMember> findByChatRoomIdsAndUserId(List<Long> chatRoomIds, Long userId) {
        return jpaChatRoomMemberRepository.findByChatRoomIdsAndUserId(chatRoomIds, userId).stream()
                .map(ChatRoomMemberEntity::toModel)
                .collect(Collectors.toMap(
                        member -> member.getChatRoom().getId(),
                        member -> member
                ));
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

    @Override
    public void updateLastReadSequenceBatch(List<Map.Entry<String, Long>> updates) {
        if (updates == null || updates.isEmpty()) return;

        String sql = "UPDATE chat_room_members SET last_read_sequence = ? " +
                "WHERE chat_room_id = ? AND user_id = ? AND left_at IS NULL " +
                "AND last_read_sequence < ?";

        List<Object[]> batchArgs = new ArrayList<>();
        for (Map.Entry<String, Long> entry : updates) {
            String[] parts = entry.getKey().split(":");
            Long chatRoomId = Long.parseLong(parts[0]);
            Long userId = Long.parseLong(parts[1]);
            Long sequence = entry.getValue();
            // lastReadSequence, chatRoomId, userId, sequence(비교용) 순서
            batchArgs.add(new Object[]{sequence, chatRoomId, userId, sequence});
        }

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
