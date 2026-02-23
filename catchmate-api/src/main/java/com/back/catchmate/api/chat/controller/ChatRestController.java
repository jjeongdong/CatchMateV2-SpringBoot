package com.back.catchmate.api.chat.controller;

import com.back.catchmate.api.chat.dto.request.ChatNotificationUpdateRequest;
import com.back.catchmate.authorization.annotation.AuthUser;
import com.back.catchmate.orchestration.chat.ChatOrchestrator;
import com.back.catchmate.orchestration.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.orchestration.chat.dto.response.ChatRoomResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.user.dto.command.UploadFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "채팅 메시지 목록 조회 (무한 스크롤)", description = "특정 채팅방의 메시지를 무한 스크롤 방식으로 조회합니다. " +
            "마지막으로 불러온 메시지 ID를 기준으로 이전 메시지들을 가져옵니다. " +
            "lastMessageId가 없으면 최신 메시지부터 조회합니다.")
    public List<ChatMessageResponse> getChatMessages(
            @AuthUser Long userId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long lastMessageId,
            @RequestParam(defaultValue = "20") int size) {
        chatOrchestrator.readChatRoom(userId, roomId);
        return chatOrchestrator.getChatHistory(userId, roomId, lastMessageId, size);
    }

    @Operation(summary = "메시지 동기화 (Sync)", description = "소켓 재연결 시 누락된 최신 메시지들을 가져옵니다.")
    @GetMapping("/rooms/{roomId}/sync")
    public ResponseEntity<List<ChatMessageResponse>> syncChatMessages(
            @AuthUser Long userId,
            @PathVariable Long roomId,
            @RequestParam Long lastMessageId,
            @RequestParam(defaultValue = "100") int size) {
        List<ChatMessageResponse> response = chatOrchestrator.syncMessages(userId, roomId, lastMessageId, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rooms/{chatRoomId}/messages/last")
    @Operation(summary = "마지막 메시지 조회", description = "특정 채팅방의 마지막 메시지를 조회합니다.")
    public ResponseEntity<ChatMessageResponse> getLastMessage(
            @AuthUser Long userId,
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

    @PutMapping("/rooms/{roomId}/notifications")
    @Operation(summary = "채팅방 알림 설정 변경", description = "특정 채팅방의 푸시 알림 수신 여부를 ON/OFF 합니다.")
    public ResponseEntity<Void> updateNotificationSetting(
            @AuthUser Long userId,
            @PathVariable Long roomId,
            @RequestBody ChatNotificationUpdateRequest request) {
        chatOrchestrator.updateNotificationSetting(userId, roomId, request.isOn());
        return ResponseEntity.ok().build();
    }

    @PatchMapping(value = "/rooms/{roomId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "채팅방 대표 이미지 수정", description = "특정 채팅방의 대표 이미지를 MultipartFile로 받아 변경합니다.")
    public ResponseEntity<Void> updateChatRoomImage(
            @AuthUser Long userId,
            @PathVariable Long roomId,
            @RequestPart(value = "chatRoomImage", required = false) MultipartFile chatRoomImage) throws IOException {

        UploadFile uploadFile = null;
        if (chatRoomImage != null && !chatRoomImage.isEmpty()) {
            uploadFile = UploadFile.builder()
                    .originalFilename(chatRoomImage.getOriginalFilename())
                    .contentType(chatRoomImage.getContentType())
                    .size(chatRoomImage.getSize())
                    .inputStream(chatRoomImage.getInputStream())
                    .build();
        }

        chatOrchestrator.updateChatRoomImage(userId, roomId, uploadFile);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/rooms/{roomId}")
    @Operation(summary = "채팅방 퇴장", description = "특정 채팅방에서 퇴장합니다. 채팅 목록 화면에서 REST API를 통해 바로 나갈 때 사용합니다.")
    public ResponseEntity<Void> leaveChatRoom(
            @AuthUser Long userId,
            @PathVariable Long roomId) {
        chatOrchestrator.leaveChatRoom(userId, roomId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/rooms/{roomId}/members/{targetUserId}")
    @Operation(summary = "채팅방 참여자 내보내기 (강퇴)", description = "채팅방의 방장이 특정 참여자를 강제로 내보냅니다.")
    public ResponseEntity<Void> kickChatRoomMember(
            @AuthUser Long userId,
            @PathVariable Long roomId,
            @PathVariable Long targetUserId) {
        chatOrchestrator.kickChatRoomMember(userId, roomId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}
