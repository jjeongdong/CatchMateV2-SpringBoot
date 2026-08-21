package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardClubInfo;
import com.back.catchmate.board.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public BoardClubInfo getClub(Long clubId) {
        return toBoardClubInfo(clubService.getClubSummary(clubId));
    }

    @Override
    public List<BoardClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::toBoardClubInfo)
                .toList();
    }

    private BoardClubInfo toBoardClubInfo(ClubSummary response) {
        return new BoardClubInfo(response.clubId(), response.name(), response.homeStadium(), response.region());
    }
}
