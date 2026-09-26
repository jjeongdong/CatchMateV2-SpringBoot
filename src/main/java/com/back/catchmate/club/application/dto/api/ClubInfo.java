package com.back.catchmate.club.application.dto.api;

import com.back.catchmate.club.domain.Club;

public record ClubInfo(Long clubId, String name, String homeStadium, String region) {
    public static ClubInfo from(Club club) {
        return new ClubInfo(club.getId(), club.getName(), club.getHomeStadium(), club.getRegion());
    }
}
