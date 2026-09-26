package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatCursorInvalidException extends BusinessException {
    public ChatCursorInvalidException() {
        super(ChatErrorCode.CHAT_CURSOR_INVALID);
    }
}
