package com.back.catchmate.chat.application.event;

import com.back.catchmate.chat.application.ChatCommandService;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatEnrollAcceptedListener {
    private final ChatCommandService chatCommandService;

    // 규칙 기본값(AFTER_COMMIT)이 아닌 동기 리스너다: 신청자 입장이 수락과 한 트랜잭션이어야 재입장 금지 등으로
    // 입장에 실패하면 수락도 취소된다 — "수락됐는데 채팅방에 없는" 상태를 만들지 않는다 (spec Q3).
    @EventListener
    public void handle(EnrollAcceptedEvent event) {
        chatCommandService.addBoardChatRoomMember(event.boardId(), event.applicantId());
    }
}
