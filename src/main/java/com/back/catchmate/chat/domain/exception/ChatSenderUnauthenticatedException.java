package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatSenderUnauthenticatedException extends BusinessException {
    public ChatSenderUnauthenticatedException() {
        super(ChatErrorCode.CHAT_SENDER_UNAUTHENTICATED);
    }
}
