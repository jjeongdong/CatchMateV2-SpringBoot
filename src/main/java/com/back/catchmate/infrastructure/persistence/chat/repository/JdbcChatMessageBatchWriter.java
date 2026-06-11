package com.back.catchmate.infrastructure.persistence.chat.repository;

import com.back.catchmate.domain.chat.model.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcChatMessageBatchWriter {
    private final JdbcTemplate jdbcTemplate;

    private static final String INSERT_SQL =
            "INSERT INTO chat_messages (chat_room_id, sender_id, content, message_type, sequence, created_at, modified_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

    public void batchInsert(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return;
        }

        jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ChatMessage msg = messages.get(i);
                Timestamp now = Timestamp.valueOf(msg.getCreatedAt());

                ps.setLong(1, msg.getChatRoom().getId());
                ps.setLong(2, msg.getSender().getId());
                ps.setString(3, msg.getContent());
                ps.setString(4, msg.getMessageType().name());
                ps.setLong(5, msg.getSequence());
                ps.setTimestamp(6, now);
                ps.setTimestamp(7, now);
            }

            @Override
            public int getBatchSize() {
                return messages.size();
            }
        });

        log.debug("채팅 메시지 {} 건 배치 INSERT 완료", messages.size());
    }
}
