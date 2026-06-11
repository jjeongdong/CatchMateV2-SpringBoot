package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.club.application.service.ClubService;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.oauth.application.port.out.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthClubFetchAdapter implements ClubFetchPort {

    private final ClubService clubService;

    @Override
    public Club getClub(Long clubId) {
        return clubService.getClub(clubId);
    }
}
