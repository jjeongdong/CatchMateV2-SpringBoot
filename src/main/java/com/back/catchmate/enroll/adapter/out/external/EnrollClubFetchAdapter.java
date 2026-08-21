package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;
import com.back.catchmate.enroll.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public EnrollClubInfo getClub(Long clubId) {
        return toEnrollClubInfo(clubService.getClubSummary(clubId));
    }

    @Override
    public List<EnrollClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::toEnrollClubInfo)
                .toList();
    }

    private EnrollClubInfo toEnrollClubInfo(ClubSummary response) {
        return new EnrollClubInfo(response.clubId(), response.name(), response.homeStadium(), response.region());
    }
}
