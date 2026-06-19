package com.back.catchmate.admin.adapter.out.external;

import com.back.catchmate.admin.application.port.out.dto.AdminClubInfo;
import com.back.catchmate.admin.application.port.out.external.ClubFetchPort;
import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AdminClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public AdminClubInfo getClub(Long clubId) {
        return fromInternalResponse(clubInternalQueryUseCase.getClub(clubId));
    }

    @Override
    public List<AdminClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    @Override
    public Optional<AdminClubInfo> findClubByName(String name) {
        return clubInternalQueryUseCase.findByName(name).map(this::fromInternalResponse);
    }

    private AdminClubInfo fromInternalResponse(ClubInternalResponse response) {
        return new AdminClubInfo(
                response.clubId(),
                response.name()
        );
    }
}
