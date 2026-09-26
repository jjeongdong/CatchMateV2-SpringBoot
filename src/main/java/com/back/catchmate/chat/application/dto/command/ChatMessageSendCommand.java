package com.back.catchmate.chat.application.dto.command;

import com.back.catchmate.chat.domain.MessageType;

public record ChatMessageSendCommand(Long chatRoomId, String content, MessageType messageType) {}
