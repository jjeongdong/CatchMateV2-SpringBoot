package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomJpaRepository extends JpaRepository<ChatRoom, Long> {
    Optional<ChatRoom> findByBoardId(Long boardId);
}
