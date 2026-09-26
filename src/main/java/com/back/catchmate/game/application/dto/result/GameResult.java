package com.back.catchmate.game.application.dto.result;

import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.domain.Game;
import java.time.LocalDateTime;

public record GameResult(
        Long gameId, LocalDateTime gameStartDate, String location, ClubView homeClub, ClubView awayClub) {

    // 경기에 구단 ID 가 비어 있거나 구단이 없으면 목록 전체를 실패시키지 않고 그 구단만 null 로 응답한다.
    public static GameResult of(Game game, ClubInfo homeClub, ClubInfo awayClub) {
        return new GameResult(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                homeClub != null ? ClubView.from(homeClub) : null,
                awayClub != null ? ClubView.from(awayClub) : null);
    }

    public record ClubView(Long clubId, String name, String homeStadium, String region) {
        static ClubView from(ClubInfo club) {
            return new ClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
        }
    }
}
