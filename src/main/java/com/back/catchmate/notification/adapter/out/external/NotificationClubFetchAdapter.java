package com.back.catchmate.notification.adapter.out.external;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.notification.application.port.out.dto.NotificationClubInfo;
import com.back.catchmate.notification.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificationClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public NotificationClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubService.getClubSummary(clubId));
    }

    @Override
    public List<NotificationClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private NotificationClubInfo fromInternalResponse(ClubSummary response) {
        return new NotificationClubInfo(
                response.clubId(),
                response.name()
        );
    }
}
