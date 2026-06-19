package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationBoardInfo;

import java.util.List;

public interface BoardFetchPort {
    NotificationBoardInfo getBoard(Long boardId);

    List<NotificationBoardInfo> getBoards(List<Long> boardIds);
}
