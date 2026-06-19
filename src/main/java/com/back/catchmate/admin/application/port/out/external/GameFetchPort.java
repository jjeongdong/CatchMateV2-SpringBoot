package com.back.catchmate.admin.application.port.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminGameInfo;

public interface GameFetchPort {
    AdminGameInfo getGame(Long gameId);
}
