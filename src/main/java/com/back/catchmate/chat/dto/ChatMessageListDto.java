package com.back.catchmate.chat.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageListDto {
    private List<ChatMessageCacheDto> messages;
}
