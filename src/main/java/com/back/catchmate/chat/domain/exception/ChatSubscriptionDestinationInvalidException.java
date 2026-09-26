package com.back.catchmate.chat.domain.exception;

import com.back.catchmate.chat.domain.ChatErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class ChatSubscriptionDestinationInvalidException extends BusinessException {
    public ChatSubscriptionDestinationInvalidException() {
        super(ChatErrorCode.CHAT_SUBSCRIPTION_DESTINATION_INVALID);
    }
}
