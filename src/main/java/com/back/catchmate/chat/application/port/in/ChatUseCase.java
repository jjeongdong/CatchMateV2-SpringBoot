package com.back.catchmate.chat.application.port.in;

import com.back.catchmate.chat.application.dto.command.ChatMessageCommand;
import com.back.catchmate.chat.application.dto.response.ChatMessageResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.user.application.dto.command.UploadFile;
import java.util.List;

public interface ChatUseCase {
    void sendMessage(Long senderId, ChatMessageCommand command);
    void enterChatRoom(Long userId, Long chatRoomId);
    void leaveChatRoom(Long userId, Long chatRoomId);
    PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size);
    void readChatRoom(Long userId, Long chatRoomId);
    List<ChatMessageResponse> getChatHistory(Long userId, Long roomId, Long lastMessageId, int size);
    List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size);
    ChatMessageResponse getLastMessage(Long chatRoomId);
    boolean canAccessChatRoom(Long userId, Long chatRoomId);
    List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId);
    void updateNotificationSetting(Long userId, Long roomId, boolean isOn);
    void updateChatRoomImage(Long userId, Long roomId, UploadFile uploadFile);
    void flushReadSequences();
    void flushMessages();
    void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId);
}
