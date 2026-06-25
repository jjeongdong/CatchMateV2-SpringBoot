package com.back.catchmate.game.adapter.out.external;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.game.application.dto.GameClubInfo;
import com.back.catchmate.game.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GameClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public List<GameClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::toGameClubInfo)
                .toList();
    }

    private GameClubInfo toGameClubInfo(ClubInternalResponse response) {
        if (response == null) return null;
        return new GameClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
