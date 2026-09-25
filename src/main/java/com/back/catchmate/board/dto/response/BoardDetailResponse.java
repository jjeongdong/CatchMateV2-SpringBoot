package com.back.catchmate.board.dto.response;

import com.back.catchmate.board.entity.Board;
import com.back.catchmate.board.entity.BoardButtonStatus;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;
import java.time.LocalDateTime;
import java.util.List;

public record BoardDetailResponse(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        String preferredGender,
        List<String> preferredAgeRange,
        LocalDateTime liftUpDate,
        boolean bookMarked,
        String buttonStatus,
        Long myEnrollId,
        Long chatRoomId,
        BoardClubView cheerClub,
        BoardGameView game,
        BoardWriterView user) {
    public static BoardDetailResponse of(
            Board board,
            boolean bookMarked,
            BoardButtonStatus buttonStatus,
            Long myEnrollId,
            Long chatRoomId,
            UserSummary user,
            ClubSummary userClub,
            ClubSummary cheerClub,
            GameSummary game,
            ClubSummary homeClub,
            ClubSummary awayClub) {
        return new BoardDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                board.getPreferredGender(),
                board.getPreferredAgeRange().asList(),
                board.getLiftUpDate(),
                bookMarked,
                buttonStatus.name(),
                myEnrollId,
                chatRoomId,
                toClubView(cheerClub),
                toGameView(game, homeClub, awayClub),
                toWriterView(user, userClub));
    }

    private static BoardClubView toClubView(ClubSummary club) {
        if (club == null) return null;
        return new BoardClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private static BoardGameView toGameView(GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        if (game == null) return null;
        return new BoardGameView(
                game.gameId(), game.gameStartDate(), game.location(), toClubView(homeClub), toClubView(awayClub));
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
                user.authority());
    }
}
