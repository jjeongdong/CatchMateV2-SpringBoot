package com.back.catchmate.application.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageListDto {
    private List<ChatMessageCacheDto> messages;
}
