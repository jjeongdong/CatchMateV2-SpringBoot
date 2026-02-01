package com.back.catchmate.api.chat.controller;

import com.back.catchmate.api.chat.dto.request.ChatMessageRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomEnterRequest;
import com.back.catchmate.api.chat.dto.request.ChatRoomLeaveRequest;
import com.back.catchmate.application.chat.ChatUseCase;
import com.back.catchmate.application.chat.dto.response.ChatMessageResponse;
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
    private final ChatUseCase chatUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 채팅 메시지 전송
     * 클라이언트 -> /pub/chat/message 로 메시지 전송
     * 서버 -> /sub/chat/room/{chatRoomId} 로 메시지 브로드캐스트
     */
    @MessageMapping("/chat/message")
    public void sendMessage(@Payload ChatMessageRequest request, Principal principal) {
        Long senderId = extractUserId(principal);

        log.info("채팅 메시지 수신 - chatRoomId: {}, senderId: {}, content: {}",
                request.getChatRoomId(), senderId, request.getContent());

        // 1. 메시지 저장 (Principal에서 추출한 senderId 사용)
        ChatMessageResponse response = chatUseCase.saveMessage(request.toCommand(senderId));

        // 2. 해당 채팅방 구독자들에게 메시지 전송
        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅 메시지 브로드캐스트 완료 - destination: {}", destination);
    }

    /**
     * 채팅방 입장
     * 클라이언트 -> /pub/chat/enter 로 채팅방 ID만 전송
     * 서버에서 입장 메시지 자동 생성
     */
    @MessageMapping("/chat/enter")
    public void enterChatRoom(@Payload ChatRoomEnterRequest request, Principal principal) {
        Long userId = extractUserId(principal);

        log.info("채팅방 입장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        // 1. 서버에서 입장 메시지 생성 및 저장
        ChatMessageResponse response = chatUseCase.enterChatRoom(userId, request.getChatRoomId());

        // 2. 해당 채팅방 구독자들에게 입장 알림 전송
        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅방 입장 알림 브로드캐스트 완료 - destination: {}, message: {}",
                destination, response.getContent());
    }

    /**
     * 채팅방 퇴장
     * 클라이언트 -> /pub/chat/leave 로 채팅방 ID만 전송
     * 서버에서 퇴장 메시지 자동 생성
     */
    @MessageMapping("/chat/leave")
    public void leaveChatRoom(@Payload ChatRoomLeaveRequest request, Principal principal) {
        Long userId = extractUserId(principal);

        log.info("채팅방 퇴장 - chatRoomId: {}, userId: {}", request.getChatRoomId(), userId);

        // 1. 서버에서 퇴장 메시지 생성 및 저장
        ChatMessageResponse response = chatUseCase.leaveChatRoom(userId, request.getChatRoomId());

        // 2. 해당 채팅방 구독자들에게 퇴장 알림 전송
        String destination = "/sub/chat/room/" + request.getChatRoomId();
        messagingTemplate.convertAndSend(destination, response);

        log.info("채팅방 퇴장 알림 브로드캐스트 완료 - destination: {}, message: {}",
                destination, response.getContent());
    }

    /**
     * Principal에서 사용자 ID 추출
     * StompAuthChannelInterceptor에서 설정한 Authentication의 Principal 사용
     */
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
