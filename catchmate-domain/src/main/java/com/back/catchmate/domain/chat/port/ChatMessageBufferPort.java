package com.back.catchmate.domain.chat.port;

import com.back.catchmate.domain.chat.model.ChatMessage;

import java.util.List;

public interface ChatMessageBufferPort {
    void buffer(ChatMessage chatMessage);

    List<ChatMessage> drainAll();

    List<ChatMessage> readByRoomId(Long chatRoomId);
}
