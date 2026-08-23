package com.back.catchmate.chat.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Map;

@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    private static final String BATCH_UPDATE_MAX_SEQUENCE = """
            UPDATE chat_rooms
            SET last_message_sequence = :sequence
            WHERE chat_room_id = :roomId
              AND (last_message_sequence IS NULL OR last_message_sequence < :sequence)
            """;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public void updateMaxSequencesBatch(Map<Long, Long> sequences) {
        if (sequences.isEmpty()) {
            return;
        }

        SqlParameterSource[] batch = sequences.entrySet().stream()
                .map(entry -> new MapSqlParameterSource()
                        .addValue("roomId", entry.getKey())
                        .addValue("sequence", entry.getValue()))
                .toArray(SqlParameterSource[]::new);

        namedParameterJdbcTemplate.batchUpdate(BATCH_UPDATE_MAX_SEQUENCE, batch);
    }
}
