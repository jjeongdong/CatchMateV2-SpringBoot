package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatRoomMemberNotFoundException extends BusinessException {
    public ChatRoomMemberNotFoundException() {
        super(ChatErrorCode.CHAT_ROOM_MEMBER_NOT_FOUND);
    }
}
