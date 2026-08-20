package com.back.catchmate.chat.adapter.in.web.controller;

import com.back.catchmate.chat.adapter.in.web.dto.request.ChatMessageRequest;
import com.back.catchmate.chat.adapter.in.web.dto.request.ChatReadRequest;
import com.back.catchmate.chat.adapter.in.web.dto.request.ChatRoomEnterRequest;
import com.back.catchmate.chat.adapter.in.web.dto.request.ChatRoomLeaveRequest;
import com.back.catchmate.chat.application.dto.response.ChatErrorResponse;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.chat.application.port.in.ChatClientCommandUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;

import java.security.Principal;
import java.util.Objects;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    // @SendToUser 가 앞에 /user 와 세션 식별자를 붙여 발신 세션 전용 목적지로 만든다.
    // 클라이언트는 /user/queue/errors 를 구독한다 (StompAuthChannelInterceptor 화이트리스트와 짝).
    private static final String ERROR_DESTINATION = "/queue/errors";

    private final ChatClientCommandUseCase chatClientCommandUseCase;

    @MessageMapping("/chat/message")
    public void sendMessage(@Valid @Payload ChatMessageRequest request, Principal principal) {
        Long senderId = extractUserId(principal);

        log.debug("채팅 메시지 수신 - chatRoomId: {}, senderId: {}, content: {}",
                request.chatRoomId(), senderId, request.content());

        chatClientCommandUseCase.sendMessage(senderId, request.toCommand(senderId));
        log.debug("채팅 메시지 처리 위임 완료 (Redis Pub/Sub 동작 중)");
    }

    @MessageMapping("/chat/enter")
    public void enterChatRoom(@Valid @Payload ChatRoomEnterRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅방 입장 - chatRoomId: {}, userId: {}", request.chatRoomId(), userId);

        chatClientCommandUseCase.enterChatRoom(userId, request.chatRoomId());
        log.info("채팅방 입장 처리 완료");
    }

    @MessageMapping("/chat/leave")
    public void leaveChatRoom(@Valid @Payload ChatRoomLeaveRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅방 퇴장 - chatRoomId: {}, userId: {}", request.chatRoomId(), userId);

        chatClientCommandUseCase.leaveChatRoom(userId, request.chatRoomId());
        log.info("채팅방 퇴장 처리 완료");
    }

    @MessageMapping("/chat/read")
    public void readChatRoom(@Valid @Payload ChatReadRequest request, Principal principal) {
        Long userId = extractUserId(principal);
        log.info("채팅 읽음 처리 - chatRoomId: {}, userId: {}", request.chatRoomId(), userId);

        chatClientCommandUseCase.readChatRoom(userId, request.chatRoomId());
    }

    // ── 전송 실패 통보 ──────────────────────────────────────────────────────────
    // @MessageMapping 에서 던져진 예외는 기본적으로 로그만 남고 삼켜져, 발신자는 메시지가 사라진 걸 알 수 없다.
    // 커밋 후 후처리 실패는 ChatMessageService.bufferAfterSend 가 자체적으로 삼키므로 여기까지 오지 않는다.
    // 따라서 이 핸들러들에 도달한 예외는 전부 "메시지가 저장되지 않은" 실패다.
    //
    // 페이로드를 ChatMessageRequest 로 받는 이유: 이 컨트롤러의 4개 요청 DTO 중 필드 상위집합이라
    // enter/leave/read 프레임에서 실패해도 chatRoomId 를 그대로 복원할 수 있다.
    // (핸들러 파라미터엔 @Valid 를 붙이지 않아 검증이 재실행되지 않는다.)

    @MessageExceptionHandler(BaseException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ChatErrorResponse handleBaseException(BaseException e, @Payload ChatMessageRequest request) {
        log.warn("채팅 요청 실패 - code: {}, chatRoomId: {}", e.getErrorCode(), request.chatRoomId());
        return ChatErrorResponse.of(request.chatRoomId(), e.getErrorCode(), isRetryable(e.getErrorCode()));
    }

    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ChatErrorResponse handleValidationException(MethodArgumentNotValidException e, @Payload ChatMessageRequest request) {
        String message = firstFieldMessage(e);

        log.warn("채팅 요청 검증 실패 - chatRoomId: {}, message: {}", request.chatRoomId(), message);
        return ChatErrorResponse.of(request.chatRoomId(), ErrorCode.BAD_REQUEST, message,
                isRetryable(ErrorCode.BAD_REQUEST));
    }

    // 역직렬화 실패(MessageConversionException)도 여기로 온다. 그 경우 @Payload 복원이 다시
    // 실패하므로 페이로드를 받지 않는다 → chatRoomId 는 null 로 나간다.
    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = ERROR_DESTINATION, broadcast = false)
    public ChatErrorResponse handleUnexpectedException(Exception e) {
        log.error("채팅 요청 처리 중 예기치 못한 오류", e);
        return ChatErrorResponse.of(null, ErrorCode.INTERNAL_SERVER_ERROR,
                isRetryable(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    // 재전송 가치는 예외 타입이 아니라 실패 성격으로 갈린다. 4xx(읽기 전용 방·비참여자·검증 실패)는 다시
    // 보내도 같은 결과지만, 5xx(Outbox 저장 실패 등 서버 일시 장애)는 트랜잭션이 롤백돼 메시지가 저장되지
    // 않았으므로 재전송이 유효하다. BaseException 에는 두 종류가 섞여 있어 타입만 보고 판단할 수 없다.
    private static boolean isRetryable(ErrorCode errorCode) {
        return errorCode.getHttpStatus().is5xxServerError();
    }

    private static String firstFieldMessage(MethodArgumentNotValidException e) {
        if (e.getBindingResult() == null) {
            return ErrorCode.BAD_REQUEST.getMessage();
        }

        return e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
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
