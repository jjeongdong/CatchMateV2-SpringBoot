package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.notification.application.port.out.dto.NotificationClubInfo;
import com.back.catchmate.notification.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public NotificationClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubInternalQueryUseCase.getClub(clubId));
    }

    @Override
    public List<NotificationClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationClubInfo fromInternalResponse(ClubInternalResponse response) {
        return new NotificationClubInfo(
                response.clubId(),
                response.name()
        );
    }
}
