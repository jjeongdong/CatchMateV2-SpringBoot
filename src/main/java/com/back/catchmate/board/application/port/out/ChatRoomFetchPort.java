package com.back.catchmate.board.application.port.out;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.user.domain.model.User;

public interface ChatRoomFetchPort {
    ChatRoom getOrCreateChatRoom(Board board);
    ChatRoomMember addMember(ChatRoom chatRoom, User user);
}
