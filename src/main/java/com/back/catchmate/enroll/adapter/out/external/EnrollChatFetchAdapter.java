package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.chat.application.service.ChatRoomMemberService;
import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.enroll.application.port.out.ChatFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollChatFetchAdapter implements ChatFetchPort {
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatRoomService chatRoomService;

    @Override
    public ChatRoomMember addMember(ChatRoom chatRoom, Long userId) {
        return chatRoomMemberService.addMember(chatRoom, userId);
    }

    @Override
    public ChatRoom getOrCreateChatRoom(Long boardId) {
        return chatRoomService.getOrCreateChatRoom(boardId);
    }
}
