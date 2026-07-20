package com.back.catchmate.chat.adapter.out.persistence.repository;

import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.chat.application.port.out.ChatMembershipCachePort;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomMemberRepository;
import com.back.catchmate.chat.application.port.out.persistence.ReadSequenceUpdate;
import com.back.catchmate.chat.adapter.out.persistence.entity.ChatRoomMemberEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepository {
    private static final String BATCH_UPDATE_LAST_READ_SEQUENCE = """
            UPDATE chat_room_members
            SET last_read_sequence = :sequence
            WHERE chat_room_id = :chatRoomId AND user_id = :userId
              AND left_at IS NULL AND last_read_sequence < :sequence
            """;

    private final JpaChatRoomMemberRepository jpaChatRoomMemberRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private final ChatMembershipCachePort chatMembershipCachePort;

    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        ChatRoomMemberEntity entity = ChatRoomMemberEntity.from(member);
        ChatRoomMember saved = jpaChatRoomMemberRepository.save(entity).toDomain();
        // 멤버십 상태 변경(add/leave/kick/read-only) → 인증 캐시 무효화 (choke point)
        chatMembershipCachePort.evict(saved.getChatRoomId(), saved.getUserId());
        return saved;
    }

    @Override
    public Optional<ChatRoomMember> findById(Long id) {
        return jpaChatRoomMemberRepository.findById(id)
                .map(ChatRoomMemberEntity::toDomain);
    }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return jpaChatRoomMemberRepository.findByChatRoomIdAndUserId(chatRoomId, userId)
                .map(ChatRoomMemberEntity::toDomain);
    }

    @Override
    public List<ChatRoomMember> findAllByUserIdAndActive(Long userId) {
        return jpaChatRoomMemberRepository.findAllByUserIdAndActive(userId).stream()
                .map(ChatRoomMemberEntity::toDomain)
                .toList();
    }

    @Override
    public List<ChatRoomMember> findAllByChatRoomIdAndActive(Long chatRoomId) {
        return jpaChatRoomMemberRepository.findAllByChatRoomIdAndActive(chatRoomId).stream()
                .map(ChatRoomMemberEntity::toDomain)
                .toList();
    }

    @Override
    public Map<Long, ChatRoomMember> findByChatRoomIdsAndUserId(List<Long> chatRoomIds, Long userId) {
        return jpaChatRoomMemberRepository.findByChatRoomIdsAndUserId(chatRoomIds, userId).stream()
                .map(ChatRoomMemberEntity::toDomain)
                .collect(Collectors.toMap(
                        ChatRoomMember::getChatRoomId,
                        member -> member,
                        (existing, replacement) -> existing
                ));
    }

    @Override
    public boolean existsByChatRoomIdAndUserIdAndActive(Long chatRoomId, Long userId) {
        return jpaChatRoomMemberRepository.existsByChatRoomIdAndUserIdAndActive(chatRoomId, userId);
    }

    @Override
    public void updateLastReadSequencesBatch(List<ReadSequenceUpdate> updates) {
        if (updates.isEmpty()) {
            return;
        }

        SqlParameterSource[] batch = updates.stream()
                .map(update -> new MapSqlParameterSource()
                        .addValue("sequence", update.sequence())
                        .addValue("chatRoomId", update.chatRoomId())
                        .addValue("userId", update.userId()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(BATCH_UPDATE_LAST_READ_SEQUENCE, batch);
    }
}
