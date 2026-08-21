package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminClubInfo;
import com.back.catchmate.admin.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public AdminClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubService.getClubSummary(clubId));
    }

    @Override
    public List<AdminClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public Optional<AdminClubInfo> findClubByName(String name) {
        return clubService.findClubSummaryByName(name).map(this::fromInternalResponse);
    }

    private AdminClubInfo fromInternalResponse(ClubSummary response) {
        return new AdminClubInfo(
                response.clubId(),
                response.name()
        );
    }
}
