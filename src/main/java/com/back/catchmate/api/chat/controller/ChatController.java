package com.back.catchmate.api.chat.controller;

import com.back.catchmate.api.chat.dto.request.ChatMessageRequest;
import com.back.catchmate.api.chat.dto.request.ChatReadRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomEnterRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomLeaveRequest;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatOrchestrator chatOrchestrator;

    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long senderId = extractUserId(principal);

        log.info("채팅 메시지 수신 - chatRoomId: {}, senderId: {}, content: {}",
                request.getChatRoomId(), senderId, request.getContent());

        chatOrchestrator.sendMessage(senderId, request.toCommand(senderId));
        log.info("채팅 메시지 처리 위임 완료 (Redis Pub/Sub 동작 중)");
    }

    @MessageMapping("/chat/enter")
    public void enterChatRoom(@Payload ChatRoomEnterRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅방 입장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        chatOrchestrator.enterChatRoom(userId, request.getChatRoomId());
        log.info("채팅방 입장 처리 완료");
    }

    @MessageMapping("/chat/leave")
    public void leaveChatRoom(@Payload ChatRoomLeaveRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅방 퇴장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        chatOrchestrator.leaveChatRoom(userId, request.getChatRoomId());
        log.info("채팅방 퇴장 처리 완료");
    }

    @MessageMapping("/chat/read")
    public void readChatRoom(@Payload ChatReadRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅 읽음 처리 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        chatOrchestrator.readChatRoom(userId, request.getChatRoomId());
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) {
            log.error("Principal is null - 인증되지 않은 요청");
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }

        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("Invalid principal format: {}", principal.getName());
            throw new BaseException(ErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
