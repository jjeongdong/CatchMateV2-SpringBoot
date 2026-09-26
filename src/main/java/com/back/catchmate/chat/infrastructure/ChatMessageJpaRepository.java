package com.back.catchmate.chat.infrastructure;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.MessageType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageJpaRepository extends JpaRepository<ChatMessage, Long> {
    Optional<ChatMessage> findTopByChatRoomIdAndMessageTypeOrderByIdDesc(Long chatRoomId, MessageType messageType);
}
