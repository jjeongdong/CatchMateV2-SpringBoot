package com.back.catchmate.game.application.port.out.external;

import com.back.catchmate.game.application.dto.GameClubInfo;

import java.util.List;

public interface ClubFetchPort {
    List<GameClubInfo> getClubs(List<Long> clubIds);
}
