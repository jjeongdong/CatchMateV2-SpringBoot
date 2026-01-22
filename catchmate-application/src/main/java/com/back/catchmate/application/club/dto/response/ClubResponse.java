package com.back.catchmate.application.club.dto.response;

import com.back.catchmate.domain.club.model.Club;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Builder
@RequiredArgsConstructor
public class ClubResponse {
    private final Long id;
    private final String name;
    private final String homeStadium;
    private final String region;

    public static ClubResponse from(Club club) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .homeStadium(club.getHomeStadium())
                .region(club.getRegion())
                .build();
    }
}
