package com.back.catchmate.chat.application.port.out;

import com.back.catchmate.chat.domain.model.ChatMessage;

import java.util.List;

public interface ChatMessageBufferPort {
    void buffer(ChatMessage chatMessage);

    List<ChatMessage> drainAll();

    List<ChatMessage> readByRoomId(Long chatRoomId);
}
