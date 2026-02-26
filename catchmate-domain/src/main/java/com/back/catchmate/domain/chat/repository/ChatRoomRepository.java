package com.back.catchmate.domain.chat.repository;

import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository {
    ChatRoom save(ChatRoom chatRoom);

    Optional<ChatRoom> findById(Long id);

    Optional<ChatRoom> findByBoardId(Long boardId);

    Optional<Long> findLastMessageSequenceById(Long id);

    DomainPage<ChatRoom> findAllByUserId(Long userId, DomainPageable pageable);

    List<ChatRoom> findAllByUserId(Long userId);

    void updateMaxSequence(Long roomId, Long sequence);

    void delete(ChatRoom chatRoom);
}
