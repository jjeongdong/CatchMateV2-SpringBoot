package com.back.catchmate.board.application.dto.response;

import com.back.catchmate.board.application.port.out.dto.BoardClubInfo;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.dto.BoardUserInfo;
import com.back.catchmate.board.domain.model.Board;

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
    public static BoardTempDetailResponse from(Board board, BoardUserInfo user, BoardClubInfo userClub, BoardClubInfo cheerClub,
                                               BoardGameInfo game, BoardClubInfo homeClub, BoardClubInfo awayClub) {
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

    private static BoardClubView toClubView(BoardClubInfo club) {
        if (club == null) return null;
        return new BoardClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private static BoardGameView toGameView(BoardGameInfo game, BoardClubInfo homeClub, BoardClubInfo awayClub) {
        if (game == null) return null;
        return new BoardGameView(
                game.gameId(),
                game.gameStartDate(),
                game.location(),
                toClubView(homeClub),
                toClubView(awayClub)
        );
    }

    private static BoardWriterView toWriterView(BoardUserInfo user, BoardClubInfo userClub) {
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
