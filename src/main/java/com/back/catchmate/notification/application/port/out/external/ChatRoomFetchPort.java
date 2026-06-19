package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationChatRecipientInfo;

import java.util.List;

public interface ChatRoomFetchPort {
    /**
     * 채팅방의 모든 활성 멤버 정보 목록 (발신자 제외).
     */
    List<NotificationChatRecipientInfo> getChatRoomRecipients(Long chatRoomId, Long excludeUserId);
}
