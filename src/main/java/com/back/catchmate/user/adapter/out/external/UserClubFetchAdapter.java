package com.back.catchmate.user.adapter.out.external;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.user.application.port.out.dto.UserClubInfo;
import com.back.catchmate.user.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public UserClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubInternalQueryUseCase.getClub(clubId));
    }

    private UserClubInfo fromInternalResponse(ClubInternalResponse response) {
        if (response == null) return null;
        return new UserClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
