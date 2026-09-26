package com.back.catchmate.club.application.dto.result;

import com.back.catchmate.club.domain.Club;

public record ClubResult(Long clubId, String name, String homeStadium, String region) {
    public static ClubResult from(Club club) {
        return new ClubResult(club.getId(), club.getName(), club.getHomeStadium(), club.getRegion());
    }
}
