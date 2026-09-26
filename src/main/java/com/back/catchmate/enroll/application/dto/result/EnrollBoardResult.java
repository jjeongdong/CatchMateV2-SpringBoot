package com.back.catchmate.enroll.application.dto.result;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;

// JSON 키(bookMarked, gameResponse, userResponse)는 옛 EnrollBoardSummary 와 같다.
public record EnrollBoardResult(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubView cheerClub,
        GameView gameResponse,
        WriterView userResponse) {
    public static EnrollBoardResult of(
            BoardInfo board, boolean bookMarked, WriterView writer, ClubView cheerClub, GameView game) {
        return new EnrollBoardResult(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
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

    // 게시글 작성자와 신청 상세의 신청자가 같은 모양이라 함께 쓴다.
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
