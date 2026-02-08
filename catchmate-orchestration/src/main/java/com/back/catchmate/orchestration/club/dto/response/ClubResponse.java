package com.back.catchmate.orchestration.club.dto.response;

import com.back.catchmate.domain.club.model.Club;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ClubResponse {
    private Long clubId;
    private String name;
    private String homeStadium;
    private String region;

    public static ClubResponse from(Club club) {
        if (club == null) return null;
        return ClubResponse.builder()
                .clubId(club.getId())
                .name(club.getName())
                .homeStadium(club.getHomeStadium())
                .region(club.getRegion())
                .build();
    }
}
