package com.back.catchmate.game.adapter.out.external;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.game.application.port.out.dto.GameClubInfo;
import com.back.catchmate.game.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public List<GameClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::toGameClubInfo)
                .toList();
    }

    private GameClubInfo toGameClubInfo(ClubSummary response) {
        if (response == null) return null;
        return new GameClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
