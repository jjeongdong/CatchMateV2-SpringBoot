package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ReadSequence;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepository {
    // 역전 방지 + 퇴장한 멤버의 읽음은 반영하지 않는다.
    private static final String BATCH_UPDATE_LAST_READ_SEQUENCE =
            """
            UPDATE chat_room_members
            SET last_read_sequence = :sequence
            WHERE chat_room_id = :chatRoomId AND user_id = :userId
              AND left_at IS NULL AND last_read_sequence < :sequence
            """;

    private final ChatRoomMemberJpaRepository chatRoomMemberJpaRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public ChatRoomMember save(ChatRoomMember member) {
        return chatRoomMemberJpaRepository.save(member);
    }

    @Override
    public Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, Long userId) {
        return chatRoomMemberJpaRepository.findByChatRoomIdAndUserId(chatRoomId, userId);
    }

    @Override
    public List<ChatRoomMember> findActiveByChatRoomId(Long chatRoomId) {
        return chatRoomMemberJpaRepository.findActiveByChatRoomId(chatRoomId);
    }

    @Override
    public List<ChatRoomMember> findActiveByChatRoomIdsAndUserId(Collection<Long> chatRoomIds, Long userId) {
        if (chatRoomIds.isEmpty()) {
            return List.of();
        }
        return chatRoomMemberJpaRepository.findActiveByChatRoomIdsAndUserId(chatRoomIds, userId);
    }

    @Override
    public void updateLastReadSequences(List<ReadSequence> readSequences) {
        if (readSequences.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = readSequences.stream()
                .map(update -> new MapSqlParameterSource()
                        .addValue("sequence", update.sequence())
                        .addValue("chatRoomId", update.chatRoomId())
                        .addValue("userId", update.userId()))
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(BATCH_UPDATE_LAST_READ_SEQUENCE, batch);
    }
}
