package com.back.catchmate.club.application.port.in;

import com.back.catchmate.club.application.dto.response.ClubInternalResponse;

import java.util.List;
import java.util.Optional;

public interface ClubInternalQueryUseCase {
    ClubInternalResponse getClub(Long clubId);

    List<ClubInternalResponse> getClubs(List<Long> clubIds);

    Optional<ClubInternalResponse> findByName(String name);
}
