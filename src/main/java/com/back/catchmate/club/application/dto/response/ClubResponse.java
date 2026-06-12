package com.back.catchmate.club.application.dto.response;

import com.back.catchmate.club.domain.model.Club;

public record ClubResponse(
        Long clubId,
        String name,
        String homeStadium,
        String region
) {
    public static ClubResponse from(Club club) {
        if (club == null) return null;
        return new ClubResponse(
                club.getId(),
                club.getName(),
                club.getHomeStadium(),
                club.getRegion()
        );
    }
}
