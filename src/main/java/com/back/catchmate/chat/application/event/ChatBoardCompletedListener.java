package com.back.catchmate.chat.application.event;

import com.back.catchmate.board.domain.event.BoardCompletedEvent;
import com.back.catchmate.chat.application.ChatCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatBoardCompletedListener {
    private final ChatCommandService chatCommandService;

    // 규칙 기본값(AFTER_COMMIT)이 아닌 동기 리스너다: 채팅방 입장이 게시글 발행과 한 트랜잭션이어야
    // "발행됐는데 작성자가 채팅방에 없는" 상태가 생기지 않는다. BEFORE_COMMIT 은 커밋 직전에 다시 이벤트
    // (입장 메시지 방송)를 발행하게 되어 쓰지 않는다 (spec §1).
    @EventListener
    public void handle(BoardCompletedEvent event) {
        chatCommandService.addBoardChatRoomMember(event.boardId(), event.ownerId());
    }
}
