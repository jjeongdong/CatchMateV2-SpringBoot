package com.back.catchmate.api.chat.controller;

import com.back.catchmate.api.chat.dto.request.ChatMessageRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomEnterRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomLeaveRequest;
import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import com.back.catchmate.orchestration.chat.dto.response.ChatMessageResponse;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatOrchestrator chatOrchestrator;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long senderId = extractUserId(principal);

        log.info("채팅 메시지 수신 - chatRoomId: {}, senderId: {}, content: {}",
                request.getChatRoomId(), senderId, request.getContent());

        ChatMessageResponse response = chatOrchestrator.sendMessage(senderId, request.toCommand(senderId));

        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅 메시지 브로드캐스트 완료 - destination: {}", destination);
    }

    @MessageMapping("/chat/enter")
    public void enterChatRoom(@Payload ChatRoomEnterRequest request, Principal principal) {
        Long userId = extractUserId(principal);

        log.info("채팅방 입장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        ChatMessageResponse response = chatOrchestrator.enterChatRoom(userId, request.getChatRoomId());

        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅방 입장 알림 브로드캐스트 완료 - destination: {}, message: {}",
                destination, response.getContent());
    }

    @MessageMapping("/chat/leave")
    public void leaveChatRoom(@Payload ChatRoomLeaveRequest request, Principal principal) {
        Long userId = extractUserId(principal);

        log.info("채팅방 퇴장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        ChatMessageResponse response = chatOrchestrator.leaveChatRoom(userId, request.getChatRoomId());

        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅방 퇴장 알림 브로드캐스트 완료 - destination: {}, message: {}",
                destination, response.getContent());
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
