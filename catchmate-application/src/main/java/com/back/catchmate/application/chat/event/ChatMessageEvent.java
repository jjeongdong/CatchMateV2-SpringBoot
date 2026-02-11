package com.back.catchmate.application.chat.event;

import com.back.catchmate.chat.enums.MessageType;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 이벤트 DTO
 * Redis Pub/Sub을 통해 전송되는 메시지 포맷
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEvent {

    private Long messageId;
    private Long roomId;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImage; // 프사 보여줘야 하니까
    private String content;
    private MessageType messageType;

    // 날짜는 String으로 변환해서 보내는게 프론트/Redis 모두에게 안전합니다.
    // 혹은 @JsonFormat을 써서 포맷팅을 명시합니다.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    /**
     * 도메인 -> 이벤트 DTO 변환 메서드
     */
    public static ChatMessageEvent from(ChatMessage domain) {
        return ChatMessageEvent.builder()
                .messageId(domain.getId())
                .roomId(domain.getChatRoom().getId())
                .senderId(domain.getSender().getId())
                .senderNickname(domain.getSender().getNickName())
                .senderProfileImage(domain.getSender().getProfileImageUrl())
                .content(domain.getContent())
                .messageType(domain.getMessageType())
                .createdAt(domain.getCreatedAt())
                .build();
    }
}
