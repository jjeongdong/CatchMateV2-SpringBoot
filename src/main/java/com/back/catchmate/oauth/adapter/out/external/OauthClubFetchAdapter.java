package com.back.catchmate.oauth.adapter.out.external;

import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.oauth.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OauthClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public void validateClubExists(Long clubId) {
        // ClubInternalQueryUseCase.getClub 이 존재하지 않으면 BaseException 을 던진다
        clubInternalQueryUseCase.getClub(clubId);
    }
}
