package com.back.catchmate.bookmark.application.dto.response;

import com.back.catchmate.bookmark.application.port.out.dto.BookmarkBoardInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkClubInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkGameInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkUserInfo;

public record BookmarkedBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        BookmarkClubResponse cheerClub,
        BookmarkGameResponse gameInfo,
        BookmarkUserResponse userInfo
) {
    public static BookmarkedBoardSummary from(
            BookmarkBoardInfo board,
            boolean bookMarked,
            BookmarkUserInfo user,
            BookmarkClubInfo userClub,
            BookmarkClubInfo cheerClub,
            BookmarkGameInfo game,
            BookmarkClubInfo homeClub,
            BookmarkClubInfo awayClub
    ) {
        return new BookmarkedBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
                bookMarked,
                cheerClub != null ? BookmarkClubResponse.from(cheerClub) : null,
                game != null ? BookmarkGameResponse.from(game, homeClub, awayClub) : null,
                user != null ? BookmarkUserResponse.from(user, userClub) : null
        );
    }

    public record BookmarkClubResponse(Long clubId, String name) {
        public static BookmarkClubResponse from(BookmarkClubInfo info) {
            return new BookmarkClubResponse(info.clubId(), info.name());
        }
    }

    public record BookmarkGameResponse(Long gameId, String homeClubName, String awayClubName, String location) {
        public static BookmarkGameResponse from(BookmarkGameInfo game, BookmarkClubInfo home, BookmarkClubInfo away) {
            return new BookmarkGameResponse(
                    game.gameId(),
                    home != null ? home.name() : null,
                    away != null ? away.name() : null,
                    game.location()
            );
        }
    }

    public record BookmarkUserResponse(Long userId, String nickName, String profileImageUrl, String clubName) {
        public static BookmarkUserResponse from(BookmarkUserInfo user, BookmarkClubInfo userClub) {
            return new BookmarkUserResponse(
                    user.userId(),
                    user.nickName(),
                    user.profileImageUrl(),
                    userClub != null ? userClub.name() : null
            );
        }
    }
}
