package com.back.catchmate.chat.dto.response;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.user.dto.response.UserSummary;

import java.time.LocalDateTime;

public record ChatRoomBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ClubSummary cheerClub,
        ChatGameSummary game,
        ChatUserSummary user
) {
    public static ChatRoomBoardSummary from(BoardSummary board, boolean bookMarked,
                                            UserSummary user, ClubSummary userClub, ClubSummary cheerClub,
                                            GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        return new ChatRoomBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                // maxPerson 은 board 쪽이 Integer 라 널이 올 수 있다. 언박싱 NPE 를 막는 기본값 0.
                board.maxPerson() != null ? board.maxPerson() : 0,
                bookMarked,
                cheerClub,
                game != null ? new ChatGameSummary(
                        game.gameId(),
                        game.gameStartDate(),
                        game.location(),
                        homeClub,
                        awayClub
                ) : null,
                user != null ? new ChatUserSummary(
                        user.userId(),
                        user.nickName(),
                        user.profileImageUrl(),
                        userClub
                ) : null
        );
    }

    public record ChatGameSummary(
            Long gameId,
            LocalDateTime gameStartDate,
            String location,
            ClubSummary homeClub,
            ClubSummary awayClub
    ) {
    }

    public record ChatUserSummary(
            Long userId,
            String nickName,
            String profileImageUrl,
            ClubSummary club
    ) {
    }
}
