package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatClubInfo;
import com.back.catchmate.chat.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public List<ChatClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private ChatClubInfo fromInternalResponse(ClubInternalResponse response) {
        if (response == null) return null;
        return new ChatClubInfo(
                response.clubId(),
                response.name(),
                response.homeStadium(),
                response.region()
        );
    }
}
