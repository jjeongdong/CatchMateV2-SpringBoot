package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.club.application.service.ClubService;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.enroll.application.port.out.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public Club getClub(Long clubId) {
        return clubService.getClub(clubId);
    }

    @Override
    public List<Club> getClubs(List<Long> clubIds) {
        return clubService.getClubs(clubIds);
    }
}
