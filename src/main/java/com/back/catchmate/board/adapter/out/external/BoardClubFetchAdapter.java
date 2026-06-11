package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.ClubFetchPort;
import com.back.catchmate.club.application.service.ClubService;
import com.back.catchmate.club.domain.model.Club;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public Club getClub(Long clubId) {
        return clubService.getClub(clubId);
    }
}
