package com.back.catchmate.chat.application.port.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatBoardInfo;

import java.util.List;

public interface BoardFetchPort {
    ChatBoardInfo getBoard(Long boardId);

    List<ChatBoardInfo> getBoards(List<Long> boardIds);
}
