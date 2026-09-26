package com.back.catchmate.chat.dto.response;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDateTime;

public record ChatRoomBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubInfo cheerClub,
        ChatGameSummary game,
        ChatUserSummary user) {
    public static ChatRoomBoardSummary from(
            BoardInfo board,
            boolean bookMarked,
            UserInfo user,
            ClubInfo userClub,
            ClubInfo cheerClub,
            GameInfo game,
            ClubInfo homeClub,
            ClubInfo awayClub) {
        return new ChatRoomBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
                bookMarked,
                cheerClub,
                game != null
                        ? new ChatGameSummary(game.gameId(), game.gameStartDate(), game.location(), homeClub, awayClub)
                        : null,
                user != null
                        ? new ChatUserSummary(user.userId(), user.nickName(), user.profileImageUrl(), userClub)
                        : null);
    }

    public record ChatGameSummary(
            Long gameId, LocalDateTime gameStartDate, String location, ClubInfo homeClub, ClubInfo awayClub) {}

    public record ChatUserSummary(Long userId, String nickName, String profileImageUrl, ClubInfo club) {}
}
