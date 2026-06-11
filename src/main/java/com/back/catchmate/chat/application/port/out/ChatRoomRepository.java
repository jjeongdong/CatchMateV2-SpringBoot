package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.chat.domain.model.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    Optional<ChatRoom> findByBoardId(Long boardId);

    Optional<Long> findLastMessageSequenceById(Long id);

    Page<ChatRoom> findAllByUserId(Long userId, Pageable pageable);

    List<ChatRoom> findAllByUserId(Long userId);

    void updateMaxSequence(Long roomId, Long sequence);

    void delete(ChatRoom chatRoom);
}
