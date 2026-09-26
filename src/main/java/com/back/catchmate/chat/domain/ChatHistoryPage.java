package com.back.catchmate.chat.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 메시지 기록 캐시(chatHistory)의 값. 발신자 닉네임·프로필까지 담아 캐시 hit 때 user 조회를 건너뛴다.
 * JSON 은 옛 ChatMessageListDto·ChatMessageCacheDto 와 같다 (배포 직후 남아 있는 캐시 항목도 읽힌다).
 * infrastructure 의 캐시 설정이 이 타입을 참조하므로 domain 에 둔다.
 */
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
