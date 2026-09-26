package com.back.catchmate.chat.presentation;

import com.back.catchmate.chat.application.dto.result.ChatMessageResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomMemberResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult;
import com.back.catchmate.chat.presentation.dto.request.ChatNotificationUpdateRequest;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "[사용자] 채팅 API")
public interface ChatRoomApiDocs {

    @Operation(summary = "내 채팅방 목록 조회", description = "참여 중인 채팅방을 마지막 메시지·안 읽은 수와 함께 오프셋 페이지로 조회합니다.")
    ResponseEntity<OffsetPageResult<ChatRoomResult>> getMyChatRooms(
            @Parameter(hidden = true) Long userId, @PositiveOrZero int page, @Min(1) @Max(100) int size);

    @Operation(
            summary = "지난 메시지 조회 (무한 스크롤)",
            description = "cursor 보다 오래된 메시지를 오래된 순으로 조회합니다. cursor 가 없으면 최신부터. "
                    + "다음 요청에는 응답의 nextCursor 를 넘깁니다. 조회하면 그 방을 읽음 처리합니다.")
    ResponseEntity<CursorPageResult<ChatMessageResult>> getMessages(
            @Parameter(hidden = true) Long userId, Long chatRoomId, String cursor, @Min(1) @Max(100) int size);

    @Operation(
            summary = "놓친 메시지 동기화",
            description = "소켓 재연결 시 lastMessageId 이후 메시지를 조회합니다. hasNext 면 nextCursor 로 이어 받습니다.")
    ResponseEntity<CursorPageResult<ChatMessageResult>> syncMessages(
            @Parameter(hidden = true) Long userId, Long chatRoomId, Long lastMessageId, @Min(1) @Max(100) int size);

    @Operation(summary = "마지막 메시지 조회", description = "채팅방의 마지막 메시지를 조회합니다. 없으면 204.")
    ResponseEntity<ChatMessageResult> getLastMessage(@Parameter(hidden = true) Long userId, Long chatRoomId);

    @Operation(summary = "참여자 목록 조회", description = "채팅방에 참여 중인 사용자 목록을 조회합니다.")
    ResponseEntity<List<ChatRoomMemberResult>> getChatRoomMembers(
            @Parameter(hidden = true) Long userId, Long chatRoomId);

    @Operation(summary = "채팅방 알림 설정 변경", description = "채팅방의 푸시 알림 수신 여부를 바꿉니다.")
    ResponseEntity<Void> updateNotificationSetting(
            @Parameter(hidden = true) Long userId, Long chatRoomId, @Valid ChatNotificationUpdateRequest request);

    @Operation(summary = "채팅방 대표 이미지 변경", description = "chatRoomImage 파트로 받은 이미지로 바꿉니다. 파트가 없으면 이미지를 지웁니다.")
    ResponseEntity<Void> updateChatRoomImage(
            @Parameter(hidden = true) Long userId, Long chatRoomId, MultipartFile chatRoomImage) throws IOException;

    @Operation(summary = "채팅방 나가기", description = "채팅방에서 퇴장합니다. 퇴장한 방에는 다시 들어올 수 없습니다.")
    ResponseEntity<Void> leaveChatRoom(@Parameter(hidden = true) Long userId, Long chatRoomId);

    @Operation(summary = "참여자 내보내기 (강퇴)", description = "방장(게시글 작성자)이 참여자를 내보냅니다.")
    ResponseEntity<Void> kickChatRoomMember(@Parameter(hidden = true) Long loginUserId, Long chatRoomId, Long userId);
}
