package com.back.catchmate.board.application.port.out;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;

public interface ChatRoomFetchPort {
    ChatRoom getOrCreateChatRoom(Long boardId);
    ChatRoomMember addMember(ChatRoom chatRoom, Long userId);
}
