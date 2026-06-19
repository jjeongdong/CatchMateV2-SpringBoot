package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardClubInfo;
import com.back.catchmate.board.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public BoardClubInfo getClub(Long clubId) {
        return toBoardClubInfo(clubInternalQueryUseCase.getClub(clubId));
    }

    @Override
    public List<BoardClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::toBoardClubInfo)
                .toList();
    }

    private BoardClubInfo toBoardClubInfo(ClubInternalResponse response) {
        return new BoardClubInfo(response.clubId(), response.name(), response.homeStadium(), response.region());
    }
}
