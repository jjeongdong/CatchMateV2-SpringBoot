package com.back.catchmate.bookmark.application.dto.result;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;

public record BookmarkedBoardResult(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubView cheerClub,
        GameView gameInfo,
        UserView userInfo) {
    // 찜 목록이라 bookMarked 는 항상 true 다.
    public static BookmarkedBoardResult of(BoardInfo board, UserView user, ClubView cheerClub, GameView game) {
        return new BookmarkedBoardResult(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
                true,
                cheerClub,
                game,
                user);
    }

    public record ClubView(Long clubId, String name) {
        public static ClubView from(ClubInfo club) {
            return club == null ? null : new ClubView(club.clubId(), club.name());
        }
    }

    public record GameView(Long gameId, String homeClubName, String awayClubName, String location) {
        public static GameView of(GameInfo game, ClubInfo homeClub, ClubInfo awayClub) {
            if (game == null) {
                return null;
            }
            return new GameView(
                    game.gameId(),
                    homeClub != null ? homeClub.name() : null,
                    awayClub != null ? awayClub.name() : null,
                    game.location());
        }
    }

    public record UserView(Long userId, String nickName, String profileImageUrl, String clubName) {
        public static UserView of(UserInfo user, ClubInfo userClub) {
            if (user == null) {
                return null;
            }
            return new UserView(
                    user.userId(), user.nickName(), user.profileImageUrl(), userClub != null ? userClub.name() : null);
        }
    }
}
