package com.back.catchmate.chat.application.port.in;

import com.back.catchmate.chat.application.dto.command.ChatMessageCommand;
import com.back.catchmate.common.upload.UploadFile;

public interface ChatClientCommandUseCase {
    void sendMessage(Long senderId, ChatMessageCommand command);

    void enterChatRoom(Long userId, Long chatRoomId);

    void leaveChatRoom(Long userId, Long chatRoomId);

    void readChatRoom(Long userId, Long chatRoomId);

    void updateNotificationSetting(Long userId, Long roomId, boolean isOn);

    void updateChatRoomImage(Long userId, Long roomId, UploadFile uploadFile);

    void kickChatRoomMember(Long hostId, Long chatRoomId, Long targetUserId);
}
