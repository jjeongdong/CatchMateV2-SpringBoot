package com.back.catchmate.board.application.dto.result;

import com.back.catchmate.board.domain.Board;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;

// JSON 키(bookMarked, gameResponse, userResponse)는 옛 BoardResponse 와 같다.
public record BoardResult(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubView cheerClub,
        GameView gameResponse,
        WriterView userResponse) {
    public static BoardResult of(
            Board board, boolean bookMarked, WriterView writer, ClubView cheerClub, GameView game) {
        return new BoardResult(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getCurrentPerson(),
                board.getMaxPerson(),
                bookMarked,
                cheerClub,
                game,
                writer);
    }

    public record ClubView(Long clubId, String name, String homeStadium, String region) {
        public static ClubView from(ClubInfo club) {
            return club == null ? null : new ClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
        }
    }

    public record GameView(
            Long gameId, LocalDateTime gameStartDate, String location, ClubView homeClub, ClubView awayClub) {
        public static GameView of(GameInfo game, ClubInfo homeClub, ClubInfo awayClub) {
            if (game == null) {
                return null;
            }
            return new GameView(
                    game.gameId(),
                    game.gameStartDate(),
                    game.location(),
                    ClubView.from(homeClub),
                    ClubView.from(awayClub));
        }
    }

    public record WriterView(
            Long userId,
            String nickName,
            String email,
            String profileImageUrl,
            char gender,
            LocalDate birthDate,
            String watchStyle,
            ClubView club,
            String authority) {
        public static WriterView of(UserInfo user, ClubInfo club) {
            if (user == null) {
                return null;
            }
            return new WriterView(
                    user.userId(),
                    user.nickName(),
                    user.email(),
                    user.profileImageUrl(),
                    user.gender() != null ? user.gender() : ' ',
                    user.birthDate(),
                    user.watchStyle(),
                    ClubView.from(club),
                    user.authority());
        }
    }
}
