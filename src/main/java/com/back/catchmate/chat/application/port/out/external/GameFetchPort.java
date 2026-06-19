package com.back.catchmate.chat.application.port.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatGameInfo;

import java.util.List;

public interface GameFetchPort {
    List<ChatGameInfo> getGames(List<Long> gameIds);
}
