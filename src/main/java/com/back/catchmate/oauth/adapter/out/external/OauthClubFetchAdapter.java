package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.oauth.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public void validateClubExists(Long clubId) {
        // ClubService.getClubSummary 가 존재하지 않으면 BaseException 을 던진다
        clubService.getClubSummary(clubId);
    }
}
