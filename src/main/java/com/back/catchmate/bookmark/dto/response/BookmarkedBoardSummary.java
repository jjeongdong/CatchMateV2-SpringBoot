package com.back.catchmate.bookmark.dto.response;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;

public record BookmarkedBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        BookmarkClubResponse cheerClub,
        BookmarkGameResponse gameInfo,
        BookmarkUserResponse userInfo) {
    public static BookmarkedBoardSummary from(
            BoardSummary board,
            boolean bookMarked,
            UserSummary user,
            ClubInfo userClub,
            ClubInfo cheerClub,
            GameSummary game,
            ClubInfo homeClub,
            ClubInfo awayClub) {
        return new BookmarkedBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
                bookMarked,
                cheerClub != null ? BookmarkClubResponse.from(cheerClub) : null,
                game != null ? BookmarkGameResponse.from(game, homeClub, awayClub) : null,
                user != null ? BookmarkUserResponse.from(user, userClub) : null);
    }

    public record BookmarkClubResponse(Long clubId, String name) {
        public static BookmarkClubResponse from(ClubInfo info) {
            return new BookmarkClubResponse(info.clubId(), info.name());
        }
    }

    public record BookmarkGameResponse(Long gameId, String homeClubName, String awayClubName, String location) {
        public static BookmarkGameResponse from(GameSummary game, ClubInfo home, ClubInfo away) {
            return new BookmarkGameResponse(
                    game.gameId(),
                    home != null ? home.name() : null,
                    away != null ? away.name() : null,
                    game.location());
        }
    }

    public record BookmarkUserResponse(Long userId, String nickName, String profileImageUrl, String clubName) {
        public static BookmarkUserResponse from(UserSummary user, ClubInfo userClub) {
            return new BookmarkUserResponse(
                    user.userId(), user.nickName(), user.profileImageUrl(), userClub != null ? userClub.name() : null);
        }
    }
}
