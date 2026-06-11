package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;

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
