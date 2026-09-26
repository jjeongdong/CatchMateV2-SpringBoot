package com.back.catchmate.chat.service;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.chat.dto.ChatMessageListDto;
import com.back.catchmate.chat.dto.response.ChatMessageResponse;
import com.back.catchmate.chat.dto.response.ChatRecipientSummary;
import com.back.catchmate.chat.dto.response.ChatRoomBoardSummary;
import com.back.catchmate.chat.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.chat.dto.response.ChatRoomResponse;
import com.back.catchmate.chat.entity.ChatMessage;
import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.entity.ChatRoomMember;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatQueryService {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final BoardService boardService;
    private final ClubQueryApi clubQueryApi;
    private final GameService gameService;
    private final UserService userService;

    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size) {
        // 채팅방 목록은 항상 최신 생성순이다. 정렬을 잃으면 목록 순서가 무작위가 된다.
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatRoom> chatRoomPage = chatRoomService.findAllByUserId(userId, pageable);

        List<Long> chatRoomIds =
                chatRoomPage.getContent().stream().map(ChatRoom::getId).toList();

        Map<Long, ChatMessage> lastMessageMap = chatMessageService.getLastMessagesByChatRoomIds(chatRoomIds);
        Map<Long, ChatRoomMember> memberMap =
                chatRoomMemberService.getChatRoomMembersByChatRoomIds(chatRoomIds, userId);

        List<Long> boardIds = chatRoomPage.getContent().stream()
                .map(ChatRoom::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<BoardSummary> boards = boardIds.isEmpty() ? List.of() : boardService.getBoardSummaries(boardIds);
        Map<Long, ChatRoomBoardSummary> boardSummaryById = buildBoardSummaries(boards).stream()
                .collect(Collectors.toMap(ChatRoomBoardSummary::boardId, Function.identity()));

        Map<Long, UserSummary> lastMessageSenderById = resolveSenders(List.copyOf(lastMessageMap.values()));

        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = Optional.ofNullable(lastMessageMap.get(chatRoom.getId()))
                            .map(msg -> ChatMessageResponse.from(msg, lastMessageSenderById.get(msg.getSenderId())))
                            .orElse(null);

                    ChatRoomMember member = memberMap.get(chatRoom.getId());

                    long unreadCount =
                            member != null ? member.calculateUnreadCount(chatRoom.getLastMessageSequence()) : 0;
                    boolean isNotificationOn = member != null && member.isNotificationOn();
                    boolean readOnly = member != null && member.isReadOnly();

                    ChatRoomBoardSummary boardSummary =
                            chatRoom.getBoardId() != null ? boardSummaryById.get(chatRoom.getBoardId()) : null;
                    return ChatRoomResponse.from(
                            chatRoom, boardSummary, lastMessage, unreadCount, isNotificationOn, readOnly);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
    }

    public List<ChatMessageResponse> getChatHistory(Long userId, Long roomId, Long lastMessageId, int size) {
        chatRoomService.validateUserInChatRoom(userId, roomId);

        ChatMessageListDto cacheDtoList = chatMessageService.getChatHistory(roomId, lastMessageId, size);

        return cacheDtoList.getMessages().stream()
                .map(dto -> new ChatMessageResponse(
                        dto.getId(),
                        dto.getRoomId(),
                        dto.getSenderId(),
                        dto.getSenderNickname(),
                        dto.getSenderProfileImageUrl(),
                        dto.getContent(),
                        MessageType.valueOf(dto.getMessageType().name()),
                        dto.getCreatedAt()))
                .toList();
    }

    public List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size) {
        chatRoomService.validateUserInChatRoom(userId, roomId);

        List<ChatMessage> syncMessages = chatMessageService.getSyncMessages(roomId, lastMessageId, size);

        Map<Long, UserSummary> senderById = resolveSenders(syncMessages);
        return syncMessages.stream()
                .map(msg -> ChatMessageResponse.from(msg, senderById.get(msg.getSenderId())))
                .toList();
    }

    public ChatMessageResponse getLastMessage(Long chatRoomId) {
        return chatMessageService
                .getLastMessage(chatRoomId)
                .map(msg -> ChatMessageResponse.from(msg, userService.getUserSummary(msg.getSenderId())))
                .orElse(null);
    }

    public boolean canAccessChatRoom(Long userId, Long chatRoomId) {
        return chatRoomService.validateUserInChatRoom(userId, chatRoomId);
    }

    public List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId) {
        List<ChatRoomMember> activeMembers = chatRoomMemberService.getChatRoomMembers(chatRoomId);

        List<Long> userIds =
                activeMembers.stream().map(ChatRoomMember::getUserId).distinct().toList();
        Map<Long, UserSummary> userById = userService.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummary::userId, Function.identity()));

        return activeMembers.stream()
                .map(member -> ChatRoomMemberResponse.from(member, userById.get(member.getUserId())))
                .toList();
    }

    /**
     * 게시글 ID에 해당하는 채팅방의 ID 반환 (없으면 empty).
     */
    public Optional<Long> findChatRoomIdByBoardId(Long boardId) {
        return chatRoomService.findByBoardId(boardId).map(ChatRoom::getId);
    }

    /**
     * 채팅방의 모든 활성 멤버 ID와 알림 설정 정보 목록 (발신자 제외).
     */
    public List<ChatRecipientSummary> getChatRoomRecipientSummaries(Long chatRoomId, Long excludeUserId) {
        return chatRoomMemberService.getChatRoomMembers(chatRoomId).stream()
                .filter(member -> !member.getUserId().equals(excludeUserId))
                .map(member -> new ChatRecipientSummary(member.getUserId(), member.isNotificationOn()))
                .toList();
    }

    private Map<Long, UserSummary> resolveSenders(List<ChatMessage> messages) {
        List<Long> senderIds =
                messages.stream().map(ChatMessage::getSenderId).distinct().toList();
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userService.getUserSummaries(senderIds).stream()
                .collect(Collectors.toMap(UserSummary::userId, Function.identity()));
    }

    private List<ChatRoomBoardSummary> buildBoardSummaries(List<BoardSummary> boards) {
        if (boards.isEmpty()) return List.of();

        List<Long> userIds = boards.stream()
                .map(BoardSummary::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(BoardSummary::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserSummary> userMap = userIds.isEmpty()
                ? Map.of()
                : userService.getUserSummaries(userIds).stream()
                        .collect(Collectors.toMap(UserSummary::userId, Function.identity()));
        Map<Long, GameSummary> gameMap = gameIds.isEmpty()
                ? Map.of()
                : gameService.getGameSummaries(gameIds).stream()
                        .collect(Collectors.toMap(GameSummary::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(BoardSummary::cheerClubId),
                        gameMap.values().stream().map(GameSummary::homeClubId),
                        gameMap.values().stream().map(GameSummary::awayClubId),
                        userMap.values().stream().map(UserSummary::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubMap = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);

        return boards.stream()
                .map(board -> toSummary(board, userMap, clubMap, gameMap))
                .toList();
    }

    private ChatRoomBoardSummary toSummary(
            BoardSummary board,
            Map<Long, UserSummary> userMap,
            Map<Long, ClubInfo> clubMap,
            Map<Long, GameSummary> gameMap) {
        UserSummary user = board.userId() != null ? userMap.get(board.userId()) : null;
        ClubInfo userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        ClubInfo cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        GameSummary game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        ClubInfo homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        ClubInfo awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return ChatRoomBoardSummary.from(board, false, user, userClub, cheerClub, game, homeClub, awayClub);
    }
}
