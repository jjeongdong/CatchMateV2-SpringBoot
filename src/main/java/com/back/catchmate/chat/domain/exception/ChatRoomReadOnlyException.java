package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatRoomReadOnlyException extends BusinessException {
    public ChatRoomReadOnlyException() {
        super(ChatErrorCode.CHAT_ROOM_READ_ONLY);
    }
}
