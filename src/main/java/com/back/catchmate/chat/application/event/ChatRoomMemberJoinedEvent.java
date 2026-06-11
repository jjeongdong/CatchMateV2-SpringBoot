package com.back.catchmate.chat.application.event;

import com.back.catchmate.user.domain.model.User;

/**
 * 채팅방에 새 멤버가 합류했음을 알리는 도메인 이벤트.
 *
 * <p>발행자는 멤버 추가까지 완료한 상태에서 본 이벤트를 발행한다.
 * 리스너는 "입장" 시스템 메시지를 저장하고 WebSocket 브로드캐스트를 트리거한다.
 */
public record ChatRoomMemberJoinedEvent(
        Long chatRoomId,
        User user
) {
    public static ChatRoomMemberJoinedEvent of(Long chatRoomId, User user) {
        return new ChatRoomMemberJoinedEvent(chatRoomId, user);
    }
}
