package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;
import com.back.catchmate.enroll.application.port.out.external.ClubFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EnrollClubFetchAdapter implements ClubFetchPort {
    private final ClubInternalQueryUseCase clubInternalQueryUseCase;

    @Override
    public EnrollClubInfo getClub(Long clubId) {
        return toEnrollClubInfo(clubInternalQueryUseCase.getClub(clubId));
    }

    @Override
    public List<EnrollClubInfo> getClubs(List<Long> clubIds) {
        return clubInternalQueryUseCase.getClubs(clubIds).stream()
                .map(this::toEnrollClubInfo)
                .toList();
    }

    private EnrollClubInfo toEnrollClubInfo(ClubInternalResponse response) {
        return new EnrollClubInfo(response.clubId(), response.name(), response.homeStadium(), response.region());
    }
}
