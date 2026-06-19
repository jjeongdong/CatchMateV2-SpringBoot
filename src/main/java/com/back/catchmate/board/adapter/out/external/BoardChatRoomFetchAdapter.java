package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.chat.application.port.in.ChatInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BoardChatRoomFetchAdapter implements ChatRoomFetchPort {
    private final ChatInternalQueryUseCase chatInternalQueryUseCase;

    @Override
    public Optional<Long> findChatRoomIdByBoardId(Long boardId) {
        return chatInternalQueryUseCase.findChatRoomIdByBoardId(boardId);
    }
}
