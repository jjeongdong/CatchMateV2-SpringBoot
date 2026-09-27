package com.back.catchmate.chat.domain;

import java.time.LocalDateTime;
import java.util.List;

// 메시지 기록 한 페이지. 발신자 닉네임·프로필까지 담는다.
public record ChatHistoryPage(List<Entry> messages) {

    public record Entry(
            Long id,
            Long roomId,
            Long senderId,
            String senderNickname,
            String senderProfileImageUrl,
            String content,
            MessageType messageType,
            LocalDateTime createdAt) {}
}
