package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.ChatRoomFetchPort;
import com.back.catchmate.chat.application.service.ChatRoomMemberService;
import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardChatRoomFetchAdapter implements ChatRoomFetchPort {
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @Override
    public ChatRoom getOrCreateChatRoom(Long boardId) {
        return chatRoomService.getOrCreateChatRoom(boardId);
    }

    @Override
    public ChatRoomMember addMember(ChatRoom chatRoom, User user) {
        return chatRoomMemberService.addMember(chatRoom, user);
    }
}
