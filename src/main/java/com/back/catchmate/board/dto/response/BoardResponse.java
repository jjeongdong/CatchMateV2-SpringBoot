package com.back.catchmate.board.dto.response;

import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.board.entity.Board;

public record BoardResponse(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        BoardClubView cheerClub,
        BoardGameView gameResponse,
        BoardWriterView userResponse
) {
    public static BoardResponse from(Board board, boolean bookMarked, UserSummary user, ClubSummary userClub,
                                     ClubSummary cheerClub, GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                bookMarked,
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
