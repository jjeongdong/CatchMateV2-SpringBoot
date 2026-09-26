package com.back.catchmate.chat.application.dto.result;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.dto.api.UserInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

// JSON 은 옛 ChatRoomResponse·ChatRoomBoardSummary 와 같다 (구단은 ClubInfo 필드 그대로 노출).
public record ChatRoomResult(
        Long chatRoomId,
        BoardView board,
        ChatMessageResult lastMessage,
        Long unreadCount,
        String chatRoomImageUrl,
        @JsonProperty("notificationOn") boolean isNotificationOn,
        boolean readOnly,
        LocalDateTime createdAt) {

    public static ChatRoomResult of(
            ChatRoom chatRoom, BoardView board, ChatMessageResult lastMessage, ChatRoomMember myMember) {
        return new ChatRoomResult(
                chatRoom.getId(),
                board,
                lastMessage,
                myMember != null ? myMember.calculateUnreadCount(chatRoom.getLastMessageSequence()) : 0L,
                chatRoom.getChatRoomImageUrl(),
                myMember != null && myMember.isNotificationOn(),
                myMember != null && myMember.isReadOnly(),
                chatRoom.getCreatedAt());
    }

    // 채팅방 목록에서는 찜 여부를 조회하지 않는다 (옛 동작: 항상 false).
    public record BoardView(
            Long boardId,
            String title,
            String content,
            int currentPerson,
            int maxPerson,
            boolean bookMarked,
            ClubInfo cheerClub,
            GameView game,
            UserView user) {
        public static BoardView of(BoardInfo board, UserView writer, ClubInfo cheerClub, GameView game) {
            return new BoardView(
                    board.boardId(),
                    board.title(),
                    board.content(),
                    board.currentPerson(),
                    board.maxPerson(),
                    false,
                    cheerClub,
                    game,
                    writer);
        }
    }

    public record GameView(
            Long gameId, LocalDateTime gameStartDate, String location, ClubInfo homeClub, ClubInfo awayClub) {
        public static GameView of(GameInfo game, ClubInfo homeClub, ClubInfo awayClub) {
            return game == null
                    ? null
                    : new GameView(game.gameId(), game.gameStartDate(), game.location(), homeClub, awayClub);
        }
    }

    public record UserView(Long userId, String nickName, String profileImageUrl, ClubInfo club) {
        public static UserView of(UserInfo user, ClubInfo club) {
            return user == null ? null : new UserView(user.userId(), user.nickName(), user.profileImageUrl(), club);
        }
    }
}
