package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;

public interface ChatFetchPort {
    ChatRoomMember addMember(ChatRoom chatRoom, Long userId);
    ChatRoom getOrCreateChatRoom(Long boardId);
}
