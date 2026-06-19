package com.back.catchmate.board.application.port.out.external;

import java.util.Optional;

public interface ChatRoomFetchPort {
    Optional<Long> findChatRoomIdByBoardId(Long boardId);
}
