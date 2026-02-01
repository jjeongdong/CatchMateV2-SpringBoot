package com.back.catchmate.api.chat.controller;

import com.back.catchmate.application.chat.ChatUseCase;
import com.back.catchmate.application.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.chat.model.ChatMessage;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.global.annotation.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "[사용자] 채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRestController {
    private final ChatUseCase chatUseCase;

    @GetMapping("/rooms/{chatRoomId}/messages")
    @Operation(summary = "채팅 메시지 목록 조회 (페이징)", description = "특정 채팅방의 메시지를 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<ChatMessageResponse>> getMessages(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        DomainPageable pageable = DomainPageable.of(page, size);
        DomainPage<ChatMessage> messagePage = chatUseCase.getMessages(chatRoomId, pageable);

        List<ChatMessageResponse> responses = messagePage.getContent().stream()
                .map(ChatMessageResponse::from)
                .toList();

        PagedResponse<ChatMessageResponse> pagedResponse = new PagedResponse<>(messagePage, responses);
        return ResponseEntity.ok(pagedResponse);
    }

    @GetMapping("/rooms/{chatRoomId}/messages/last")
    @Operation(summary = "마지막 메시지 조회", description = "특정 채팅방의 마지막 메시지를 조회합니다.")
    public ResponseEntity<ChatMessageResponse> getLastMessage(@AuthUser Long userId,
                                                              @PathVariable Long chatRoomId) {
        ChatMessageResponse response = chatUseCase.getLastMessage(chatRoomId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }
}
