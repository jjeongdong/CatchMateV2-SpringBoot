package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatClubInfo;
import com.back.catchmate.chat.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatClubFetchAdapter implements ClubFetchPort {
    private final ClubService clubService;

    @Override
    public List<ChatClubInfo> getClubs(List<Long> clubIds) {
        return clubService.getClubSummaries(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private ChatClubInfo fromInternalResponse(ClubSummary response) {
        if (response == null) return null;
        return new ChatClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
