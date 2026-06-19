package com.back.catchmate.chat.application.port.in;

import com.back.catchmate.chat.application.dto.response.ChatRecipientInternalResponse;

import java.util.List;
import java.util.Optional;

public interface ChatInternalQueryUseCase {
    /**
     * 게시글 ID에 해당하는 채팅방의 ID 반환 (없으면 empty).
     * 도메인 모델을 노출하지 않기 위해 식별자만 반환.
     */
    Optional<Long> findChatRoomIdByBoardId(Long boardId);

    /**
     * 채팅방의 모든 활성 멤버 ID와 알림 설정 정보 목록 (발신자 제외).
     */
    List<ChatRecipientInternalResponse> getChatRoomRecipients(Long chatRoomId, Long excludeUserId);
}
