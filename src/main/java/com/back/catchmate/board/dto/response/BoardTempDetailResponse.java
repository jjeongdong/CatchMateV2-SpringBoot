package com.back.catchmate.board.dto.response;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.board.entity.Board;

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
        BoardWriterView user
) {
    public static BoardTempDetailResponse from(Board board, UserSummary user, ClubSummary userClub, ClubSummary cheerClub,
                                               GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
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
                toWriterView(user, userClub)
        );
    }

    private static BoardClubView toClubView(ClubSummary club) {
        if (club == null) return null;
        return new BoardClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private static BoardGameView toGameView(GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        if (game == null) return null;
        return new BoardGameView(
                game.gameId(),
                game.gameStartDate(),
                game.location(),
                toClubView(homeClub),
                toClubView(awayClub)
        );
    }

    private static BoardWriterView toWriterView(UserSummary user, ClubSummary userClub) {
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
                user.authority()
        );
    }
}
