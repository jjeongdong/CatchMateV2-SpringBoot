package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatRoomNotHostException extends BusinessException {
    public ChatRoomNotHostException() {
        super(ChatErrorCode.CHAT_ROOM_NOT_HOST);
    }
}
