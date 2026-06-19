package com.back.catchmate.notification.application.port.out.external;

import com.back.catchmate.notification.application.port.out.dto.NotificationGameInfo;

import java.util.List;

public interface GameFetchPort {
    NotificationGameInfo getGame(Long gameId);

    List<NotificationGameInfo> getGames(List<Long> gameIds);
}
