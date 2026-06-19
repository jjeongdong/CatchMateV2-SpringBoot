package com.back.catchmate.chat.application.event;

/**
 * 채팅방에 새 메시지가 저장되었음을 알리는 사실 이벤트.
 * <p>알림 대상자 / 닉네임 / 표시 포맷 등 구독자 관심사는 페이로드에 담지 않는다.
 */
public record ChatMessageSentEvent(
        Long chatRoomId,
        Long messageId,
        Long senderId,
        String content
) {
    public static ChatMessageSentEvent of(Long chatRoomId, Long messageId, Long senderId, String content) {
        return new ChatMessageSentEvent(chatRoomId, messageId, senderId, content);
    }
}
