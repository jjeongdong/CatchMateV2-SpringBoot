package com.back.catchmate.game.dto.response;

import com.back.catchmate.club.application.dto.api.ClubInfo;

/**
 * 경기 선택 응답에 임베드되는 구단 요약.
 */
public record GameClubView(Long clubId, String name, String homeStadium, String region) {
    public static GameClubView from(ClubInfo club) {
        if (club == null) return null;
        return new GameClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }
}
