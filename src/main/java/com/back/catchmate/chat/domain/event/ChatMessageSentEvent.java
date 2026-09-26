package com.back.catchmate.chat.domain.event;

/**
 * 채팅방에 사용자 메시지가 저장되었음을 알리는 사실 이벤트. 메시지 INSERT 와 같은 트랜잭션에서 발행된다.
 * 알림 대상자·닉네임·표시 포맷 등 구독자 관심사는 담지 않는다.
 */
public record ChatMessageSentEvent(Long chatRoomId, Long messageId, Long senderId, String content) {}
