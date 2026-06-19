package com.back.catchmate.board.application.port.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardClubInfo;

import java.util.List;

public interface ClubFetchPort {
    BoardClubInfo getClub(Long clubId);

    List<BoardClubInfo> getClubs(List<Long> clubIds);
}
