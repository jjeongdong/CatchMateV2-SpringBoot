package com.back.catchmate.enroll.application.port.out;

import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.user.domain.model.User;

public interface ChatFetchPort {
    ChatRoomMember addMember(ChatRoom chatRoom, User user);
    ChatRoom getOrCreateChatRoom(Long boardId);
}
