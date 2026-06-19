package com.back.catchmate.club.application.service;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;
import com.back.catchmate.club.application.port.in.ClubInternalQueryUseCase;
import com.back.catchmate.club.domain.model.Club;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubInternalQueryService implements ClubInternalQueryUseCase {
    private final ClubReader clubReader;

    @Override
    public ClubInternalResponse getClub(Long clubId) {
        return toInternalResponse(clubReader.getClub(clubId));
    }

    @Override
    public List<ClubInternalResponse> getClubs(List<Long> clubIds) {
        return clubReader.getClubs(clubIds).stream()
                .map(this::toInternalResponse)
                .toList();
    }

    @Override
    public Optional<ClubInternalResponse> findByName(String name) {
        return clubReader.findByName(name).map(this::toInternalResponse);
    }

    private ClubInternalResponse toInternalResponse(Club club) {
        return new ClubInternalResponse(club.getId(), club.getName(), club.getHomeStadium(), club.getRegion());
    }
}
