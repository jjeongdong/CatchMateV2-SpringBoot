package com.back.catchmate.api.chat.controller;

import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import com.back.catchmate.orchestration.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
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
    private final ChatOrchestrator chatOrchestrator;

    @GetMapping("/rooms")
    @Operation(summary = "내가 속한 채팅방 목록 조회 (페이징)", description = "현재 사용자가 참여 중인 모든 채팅방을 조회합니다. 각 채팅방의 마지막 메시지도 함께 반환됩니다.")
    public ResponseEntity<PagedResponse<ChatRoomResponse>> getMyChatRooms(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(chatOrchestrator.getMyChatRooms(userId, page, size));
    }

    @GetMapping("/rooms/{chatRoomId}/messages")
    @Operation(summary = "채팅 메시지 목록 조회 (페이징)", description = "특정 채팅방의 메시지를 페이징하여 조회합니다.")
    public ResponseEntity<PagedResponse<ChatMessageResponse>> getMessages(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(chatOrchestrator.getMessages(chatRoomId, page, size));
    }

    @GetMapping("/rooms/{chatRoomId}/messages/last")
    @Operation(summary = "마지막 메시지 조회", description = "특정 채팅방의 마지막 메시지를 조회합니다.")
    public ResponseEntity<ChatMessageResponse> getLastMessage(@AuthUser Long userId,
                                                              @PathVariable Long chatRoomId) {
        ChatMessageResponse response = chatOrchestrator.getLastMessage(chatRoomId);
        if (response == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{chatRoomId}/members")
    @Operation(summary = "채팅방 참여자 목록 조회", description = "특정 채팅방에 현재 참여 중인 사용자 목록을 조회합니다.")
    public ResponseEntity<List<ChatRoomMemberResponse>> getChatRoomMembers(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId) {

        return ResponseEntity.ok(chatOrchestrator.getChatRoomMembers(chatRoomId));
    }
}
