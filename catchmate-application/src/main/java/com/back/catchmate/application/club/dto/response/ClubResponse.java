package com.back.catchmate.application.club.dto.response;

import com.back.catchmate.domain.club.model.Club;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClubResponse {
    private Long id;
    private String name;
    private String homeStadium;
    private String region;

    public static ClubResponse from(Club club) {
        return ClubResponse.builder()
                .id(club.getId())
                .name(club.getName())
                .homeStadium(club.getHomeStadium())
                .region(club.getRegion())
                .build();
    }
}
