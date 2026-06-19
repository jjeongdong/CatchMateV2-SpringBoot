package com.back.catchmate.chat.application.port.in;

import com.back.catchmate.chat.application.dto.response.ChatMessageResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomResponse;
import com.back.catchmate.common.response.PagedResponse;

import java.util.List;

public interface ChatClientQueryUseCase {
    PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size);

    List<ChatMessageResponse> getChatHistory(Long userId, Long roomId, Long lastMessageId, int size);

    List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size);

    ChatMessageResponse getLastMessage(Long chatRoomId);

    boolean canAccessChatRoom(Long userId, Long chatRoomId);

    List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId);
}
