package com.back.catchmate.chat.repository;

import com.back.catchmate.chat.dto.ReadSequenceUpdate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;

@RequiredArgsConstructor
public class ChatRoomMemberRepositoryImpl implements ChatRoomMemberRepositoryCustom {
    private static final String BATCH_UPDATE_LAST_READ_SEQUENCE = """
            UPDATE chat_room_members
            SET last_read_sequence = :sequence
            WHERE chat_room_id = :chatRoomId AND user_id = :userId
              AND left_at IS NULL AND last_read_sequence < :sequence
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

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
