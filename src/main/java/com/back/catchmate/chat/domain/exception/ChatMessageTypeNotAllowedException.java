package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatMessageTypeNotAllowedException extends BusinessException {
    public ChatMessageTypeNotAllowedException() {
        super(ChatErrorCode.CHAT_MESSAGE_TYPE_NOT_ALLOWED);
    }
}
