package com.back.catchmate.chat.infrastructure;

import static com.back.catchmate.chat.domain.QChatRoom.chatRoom;
import static com.back.catchmate.chat.domain.QChatRoomMember.chatRoomMember;

import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.exception.ChatRoomNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {
    // 역전 방지: 되돌린 버퍼가 늦게 반영돼도 더 큰 값을 덮어쓰지 않는다.
    private static final String BATCH_UPDATE_MAX_SEQUENCE =
            """
            UPDATE chat_rooms
            SET last_message_sequence = :sequence
            WHERE chat_room_id = :roomId
              AND (last_message_sequence IS NULL OR last_message_sequence < :sequence)
            """;

    private final ChatRoomJpaRepository chatRoomJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public ChatRoom save(ChatRoom newChatRoom) {
        return chatRoomJpaRepository.save(newChatRoom);
    }

    @Override
    public ChatRoom getById(Long chatRoomId) {
        return chatRoomJpaRepository.findById(chatRoomId).orElseThrow(ChatRoomNotFoundException::new);
    }

    @Override
    public ChatRoom getReference(Long chatRoomId) {
        return chatRoomJpaRepository.getReferenceById(chatRoomId);
    }

    @Override
    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return chatRoomJpaRepository.findByBoardId(boardId);
    }

    @Override
    public List<ChatRoom> findAllByMemberUserId(Long userId, long offset, int limit) {
        return jpaQueryFactory
                .selectFrom(chatRoom)
                .join(chatRoomMember)
                .on(chatRoomMember.chatRoom.id.eq(chatRoom.id))
                .where(chatRoomMember.userId.eq(userId), chatRoomMember.leftAt.isNull())
                .orderBy(chatRoom.createdAt.desc(), chatRoom.id.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public long countByMemberUserId(Long userId) {
        Long count = jpaQueryFactory
                .select(chatRoom.count())
                .from(chatRoom)
                .join(chatRoomMember)
                .on(chatRoomMember.chatRoom.id.eq(chatRoom.id))
                .where(chatRoomMember.userId.eq(userId), chatRoomMember.leftAt.isNull())
                .fetchOne();
        return count != null ? count : 0L;
    }

    @Override
    public void updateMaxSequences(Map<Long, Long> sequenceByChatRoomId) {
        if (sequenceByChatRoomId.isEmpty()) {
            return;
        }
        SqlParameterSource[] batch = sequenceByChatRoomId.entrySet().stream()
                .map(entry -> new MapSqlParameterSource()
                        .addValue("roomId", entry.getKey())
                        .addValue("sequence", entry.getValue()))
                .toArray(SqlParameterSource[]::new);
        namedParameterJdbcTemplate.batchUpdate(BATCH_UPDATE_MAX_SEQUENCE, batch);
    }
}
