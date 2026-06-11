package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.chat.application.service.ChatRoomMemberService;
import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.enroll.application.port.out.ChatFetchPort;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollChatFetchAdapter implements ChatFetchPort {
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatRoomService chatRoomService;

    @Override
    public ChatRoomMember addMember(ChatRoom chatRoom, User user) {
        return chatRoomMemberService.addMember(chatRoom, user);
    }

    @Override
    public ChatRoom getOrCreateChatRoom(Board board) {
        return chatRoomService.getOrCreateChatRoom(board);
    }
}
