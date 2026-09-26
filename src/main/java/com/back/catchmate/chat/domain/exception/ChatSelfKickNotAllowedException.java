package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatSelfKickNotAllowedException extends BusinessException {
    public ChatSelfKickNotAllowedException() {
        super(ChatErrorCode.CHAT_SELF_KICK_NOT_ALLOWED);
    }
}
