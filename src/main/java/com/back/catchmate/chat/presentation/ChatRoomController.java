package com.back.catchmate.chat.presentation;

import com.back.catchmate.chat.application.ChatCommandService;
import com.back.catchmate.chat.application.ChatQueryService;
import com.back.catchmate.chat.application.dto.result.ChatMessageResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomMemberResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult;
import com.back.catchmate.chat.presentation.dto.request.ChatNotificationUpdateRequest;
import com.back.catchmate.global.authorization.annotation.AuthUser;
import com.back.catchmate.global.infrastructure.upload.UploadFile;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import java.io.IOException;
import java.util.List;
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

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatRoomController implements ChatRoomApiDocs {
    private final ChatCommandService chatCommandService;
    private final ChatQueryService chatQueryService;

    @Override
    @GetMapping
    public ResponseEntity<OffsetPageResult<ChatRoomResult>> getMyChatRooms(
            @AuthUser Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatQueryService.getMyChatRooms(userId, page, size));
    }

    @Override
    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<CursorPageResult<ChatMessageResult>> getMessages(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(chatQueryService.getMessages(userId, chatRoomId, cursor, size));
    }

    @Override
    @GetMapping("/{chatRoomId}/messages/sync")
    public ResponseEntity<CursorPageResult<ChatMessageResult>> syncMessages(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId,
            @RequestParam Long lastMessageId,
            @RequestParam(defaultValue = "100") int size) {
        return ResponseEntity.ok(chatQueryService.syncMessages(userId, chatRoomId, lastMessageId, size));
    }

    @Override
    @GetMapping("/{chatRoomId}/messages/last")
    public ResponseEntity<ChatMessageResult> getLastMessage(@AuthUser Long userId, @PathVariable Long chatRoomId) {
        return chatQueryService
                .getLastMessage(userId, chatRoomId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Override
    @GetMapping("/{chatRoomId}/members")
    public ResponseEntity<List<ChatRoomMemberResult>> getChatRoomMembers(
            @AuthUser Long userId, @PathVariable Long chatRoomId) {
        return ResponseEntity.ok(chatQueryService.getChatRoomMembers(userId, chatRoomId));
    }

    @Override
    @PutMapping("/{chatRoomId}/notifications")
    public ResponseEntity<Void> updateNotificationSetting(
            @AuthUser Long userId, @PathVariable Long chatRoomId, @RequestBody ChatNotificationUpdateRequest request) {
        chatCommandService.updateNotificationSetting(userId, chatRoomId, request.notificationOn());
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping(value = "/{chatRoomId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateChatRoomImage(
            @AuthUser Long userId,
            @PathVariable Long chatRoomId,
            @RequestPart(value = "chatRoomImage", required = false) MultipartFile chatRoomImage)
            throws IOException {
        // Application 이 Spring Web 의 MultipartFile 을 모르도록 표현 계층에서 UploadFile 로 바꾼다.
        UploadFile uploadFile = chatRoomImage == null || chatRoomImage.isEmpty()
                ? null
                : new UploadFile(
                        chatRoomImage.getOriginalFilename(),
                        chatRoomImage.getContentType(),
                        chatRoomImage.getInputStream(),
                        chatRoomImage.getSize());
        chatCommandService.updateChatRoomImage(userId, chatRoomId, uploadFile);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{chatRoomId}/members/me")
    public ResponseEntity<Void> leaveChatRoom(@AuthUser Long userId, @PathVariable Long chatRoomId) {
        chatCommandService.leaveChatRoom(userId, chatRoomId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{chatRoomId}/members/{userId}")
    public ResponseEntity<Void> kickChatRoomMember(
            @AuthUser Long loginUserId, @PathVariable Long chatRoomId, @PathVariable Long userId) {
        chatCommandService.kickChatRoomMember(loginUserId, chatRoomId, userId);
        return ResponseEntity.noContent().build();
    }
}
