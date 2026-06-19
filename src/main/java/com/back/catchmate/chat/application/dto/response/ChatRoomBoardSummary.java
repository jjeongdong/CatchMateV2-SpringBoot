package com.back.catchmate.chat.application.dto.response;

import com.back.catchmate.chat.application.port.out.dto.ChatBoardInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatClubInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatGameInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;

import java.time.LocalDateTime;

public record ChatRoomBoardSummary(
        Long boardId,
        String title,
        String content,
        int currentPerson,
        int maxPerson,
        boolean bookMarked,
        ChatClubInfo cheerClub,
        ChatGameSummary game,
        ChatUserSummary user
) {
    public static ChatRoomBoardSummary from(ChatBoardInfo board, boolean bookMarked,
                                            ChatUserInfo user, ChatClubInfo userClub, ChatClubInfo cheerClub,
                                            ChatGameInfo game, ChatClubInfo homeClub, ChatClubInfo awayClub) {
        return new ChatRoomBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
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
            ChatClubInfo homeClub,
            ChatClubInfo awayClub
    ) {
    }

    public record ChatUserSummary(
            Long userId,
            String nickName,
            String profileImageUrl,
            ChatClubInfo club
    ) {
    }
}
