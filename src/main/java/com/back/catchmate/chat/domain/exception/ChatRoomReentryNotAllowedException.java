package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatRoomReentryNotAllowedException extends BusinessException {
    public ChatRoomReentryNotAllowedException() {
        super(ChatErrorCode.CHAT_ROOM_REENTRY_NOT_ALLOWED);
    }
}
