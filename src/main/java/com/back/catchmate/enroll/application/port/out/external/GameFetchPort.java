package com.back.catchmate.enroll.application.port.out.external;

import com.back.catchmate.enroll.application.port.out.dto.EnrollGameInfo;

import java.util.List;

public interface GameFetchPort {
    List<EnrollGameInfo> getGames(List<Long> gameIds);
}
