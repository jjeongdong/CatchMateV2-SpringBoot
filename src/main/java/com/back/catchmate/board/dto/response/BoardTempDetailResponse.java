package com.back.catchmate.board.dto.response;

import com.back.catchmate.board.entity.Board;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;
import java.util.List;

public record BoardTempDetailResponse(
        Long boardId,
        String title,
        String content,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        BoardClubView cheerClub,
        BoardGameView game,
        BoardWriterView user) {
    public static BoardTempDetailResponse from(
            Board board,
            UserSummary user,
            ClubInfo userClub,
            ClubInfo cheerClub,
            GameSummary game,
            ClubInfo homeClub,
            ClubInfo awayClub) {
        if (board == null) {
            return null;
        }

        return new BoardTempDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                toClubView(cheerClub),
                toGameView(game, homeClub, awayClub),
                toWriterView(user, userClub));
    }

    private static BoardClubView toClubView(ClubInfo club) {
        if (club == null) return null;
        return new BoardClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private static BoardGameView toGameView(GameSummary game, ClubInfo homeClub, ClubInfo awayClub) {
        if (game == null) return null;
        return new BoardGameView(
                game.gameId(), game.gameStartDate(), game.location(), toClubView(homeClub), toClubView(awayClub));
    }

    private static BoardWriterView toWriterView(UserSummary user, ClubInfo userClub) {
        if (user == null) return null;
        return new BoardWriterView(
                user.userId(),
                user.nickName(),
                user.email(),
                user.profileImageUrl(),
                user.gender() != null ? user.gender() : ' ',
                user.birthDate(),
                user.watchStyle(),
                toClubView(userClub),
                user.authority());
    }
}
