package com.back.catchmate.chat.adapter.out.persistence.repository;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.application.port.out.persistence.ChatRoomRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.chat.adapter.out.persistence.entity.ChatRoomEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepository {
    private static final String BATCH_UPDATE_MAX_SEQUENCE = """
            UPDATE chat_rooms
            SET last_message_sequence = :sequence
            WHERE chat_room_id = :roomId
              AND (last_message_sequence IS NULL OR last_message_sequence < :sequence)
            """;

    private final JpaChatRoomRepository jpaChatRoomRepository;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public ChatRoom save(ChatRoom chatRoom) {
        ChatRoomEntity entity = ChatRoomEntity.from(chatRoom);
        return jpaChatRoomRepository.save(entity).toDomain();
    }

    @Override
    public Optional<ChatRoom> findById(Long id) {
        return jpaChatRoomRepository.findById(id)
                .map(ChatRoomEntity::toDomain);
    }

    @Override
    public Optional<ChatRoom> findByBoardId(Long boardId) {
        return jpaChatRoomRepository.findByBoardId(boardId)
                .map(ChatRoomEntity::toDomain);
    }

    @Override
    public Optional<Long> findLastMessageSequenceById(Long id) {
        return jpaChatRoomRepository.findLastMessageSequenceById(id);
    }

    @Override
    public Page<ChatRoom> findAllByUserId(Long userId, Pageable pageable) {
        PageRequest sortedPageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ChatRoomEntity> entityPage = jpaChatRoomRepository.findAllByUserIdWithPaging(userId, sortedPageRequest);

        return entityPage.map(ChatRoomEntity::toDomain);
    }

    @Override
    public List<ChatRoom> findAllByUserId(Long userId) {
        return jpaChatRoomRepository.findAllByUserId(userId).stream()
                .map(ChatRoomEntity::toDomain)
                .toList();
    }

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
