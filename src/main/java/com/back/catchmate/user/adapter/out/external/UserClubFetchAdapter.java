package com.back.catchmate.user.adapter.out.external;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.user.application.port.out.dto.UserClubInfo;
import com.back.catchmate.user.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public UserClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubService.getClubSummary(clubId));
    }

    private UserClubInfo fromInternalResponse(ClubSummary response) {
        if (response == null) return null;
        return new UserClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
