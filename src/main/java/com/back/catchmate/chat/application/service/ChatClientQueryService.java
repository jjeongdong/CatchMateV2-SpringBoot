package com.back.catchmate.chat.application.service;

import com.back.catchmate.chat.application.dto.ChatMessageListDto;
import com.back.catchmate.chat.application.dto.response.ChatMessageResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomBoardSummary;
import com.back.catchmate.chat.application.dto.response.ChatRoomMemberResponse;
import com.back.catchmate.chat.application.dto.response.ChatRoomResponse;
import com.back.catchmate.chat.application.port.in.ChatClientQueryUseCase;
import com.back.catchmate.chat.application.port.out.dto.ChatBoardInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatClubInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatGameInfo;
import com.back.catchmate.chat.application.port.out.dto.ChatUserInfo;
import com.back.catchmate.chat.application.port.out.external.BoardFetchPort;
import com.back.catchmate.chat.application.port.out.external.ClubFetchPort;
import com.back.catchmate.chat.application.port.out.external.GameFetchPort;
import com.back.catchmate.chat.application.port.out.external.UserFetchPort;
import com.back.catchmate.chat.domain.enums.MessageType;
import com.back.catchmate.chat.domain.model.ChatMessage;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.chat.domain.model.ChatRoomMember;
import com.back.catchmate.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatClientQueryService implements ChatClientQueryUseCase {
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final BoardFetchPort boardFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;

    @Override
    public PagedResponse<ChatRoomResponse> getMyChatRooms(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ChatRoom> chatRoomPage = chatRoomService.findAllByUserId(userId, pageable);

        List<Long> chatRoomIds = chatRoomPage.getContent().stream()
                .map(ChatRoom::getId)
                .toList();

        Map<Long, ChatMessage> lastMessageMap = chatMessageService.getLastMessagesByChatRoomIds(chatRoomIds);
        Map<Long, ChatRoomMember> memberMap = chatRoomMemberService.getChatRoomMembersByChatRoomIds(chatRoomIds, userId);

        List<Long> boardIds = chatRoomPage.getContent().stream()
                .map(ChatRoom::getBoardId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<ChatBoardInfo> boards = boardIds.isEmpty() ? List.of() : boardFetchPort.getBoards(boardIds);
        Map<Long, ChatRoomBoardSummary> boardSummaryById = buildBoardSummaries(boards).stream()
                .collect(Collectors.toMap(ChatRoomBoardSummary::boardId, Function.identity()));

        Map<Long, ChatUserInfo> lastMessageSenderById = resolveSenders(List.copyOf(lastMessageMap.values()));

        List<ChatRoomResponse> responses = chatRoomPage.getContent().stream()
                .map(chatRoom -> {
                    ChatMessageResponse lastMessage = Optional.ofNullable(lastMessageMap.get(chatRoom.getId()))
                            .map(msg -> ChatMessageResponse.from(msg, lastMessageSenderById.get(msg.getSenderId())))
                            .orElse(null);

                    ChatRoomMember member = memberMap.get(chatRoom.getId());

                    long unreadCount = member != null
                            ? member.calculateUnreadCount(chatRoom.getLastMessageSequence())
                            : 0;
                    boolean isNotificationOn = member != null && member.isNotificationOn();
                    boolean readOnly = member != null && member.isReadOnly();

                    ChatRoomBoardSummary boardSummary = chatRoom.getBoardId() != null ? boardSummaryById.get(chatRoom.getBoardId()) : null;
                    return ChatRoomResponse.from(chatRoom, boardSummary, lastMessage, unreadCount, isNotificationOn, readOnly);
                })
                .toList();

        return new PagedResponse<>(chatRoomPage, responses);
    }

    @Override
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
                        dto.getCreatedAt()
                ))
                .toList();
    }

    @Override
    public List<ChatMessageResponse> syncMessages(Long userId, Long roomId, Long lastMessageId, int size) {
        chatRoomService.validateUserInChatRoom(userId, roomId);

        List<ChatMessage> syncMessages = chatMessageService.getSyncMessages(roomId, lastMessageId, size);

        Map<Long, ChatUserInfo> senderById = resolveSenders(syncMessages);
        return syncMessages.stream()
                .map(msg -> ChatMessageResponse.from(msg, senderById.get(msg.getSenderId())))
                .toList();
    }

    @Override
    public ChatMessageResponse getLastMessage(Long chatRoomId) {
        return chatMessageService.getLastMessage(chatRoomId)
                .map(msg -> ChatMessageResponse.from(msg, userFetchPort.getUser(msg.getSenderId())))
                .orElse(null);
    }

    @Override
    public boolean canAccessChatRoom(Long userId, Long chatRoomId) {
        return chatRoomService.validateUserInChatRoom(userId, chatRoomId);
    }

    @Override
    public List<ChatRoomMemberResponse> getChatRoomMembers(Long chatRoomId) {
        List<ChatRoomMember> activeMembers = chatRoomMemberService.getChatRoomMembers(chatRoomId);

        List<Long> userIds = activeMembers.stream()
                .map(ChatRoomMember::getUserId)
                .distinct()
                .toList();
        Map<Long, ChatUserInfo> userById = userFetchPort.getUsers(userIds).stream()
                .collect(Collectors.toMap(ChatUserInfo::userId, Function.identity()));

        return activeMembers.stream()
                .map(member -> ChatRoomMemberResponse.from(member, userById.get(member.getUserId())))
                .toList();
    }

    private Map<Long, ChatUserInfo> resolveSenders(List<ChatMessage> messages) {
        List<Long> senderIds = messages.stream()
                .map(ChatMessage::getSenderId)
                .distinct()
                .toList();
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userFetchPort.getUsers(senderIds).stream()
                .collect(Collectors.toMap(ChatUserInfo::userId, Function.identity()));
    }

    private List<ChatRoomBoardSummary> buildBoardSummaries(List<ChatBoardInfo> boards) {
        if (boards.isEmpty()) return List.of();

        List<Long> userIds = boards.stream()
                .map(ChatBoardInfo::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(ChatBoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ChatUserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userFetchPort.getUsers(userIds).stream()
                        .collect(Collectors.toMap(ChatUserInfo::userId, Function.identity()));
        Map<Long, ChatGameInfo> gameMap = gameIds.isEmpty() ? Map.of() :
                gameFetchPort.getGames(gameIds).stream()
                        .collect(Collectors.toMap(ChatGameInfo::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(ChatBoardInfo::cheerClubId),
                        gameMap.values().stream().map(ChatGameInfo::homeClubId),
                        gameMap.values().stream().map(ChatGameInfo::awayClubId),
                        userMap.values().stream().map(ChatUserInfo::clubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ChatClubInfo> clubMap = clubIds.isEmpty() ? Map.of() :
                clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(ChatClubInfo::clubId, Function.identity()));

        return boards.stream()
                .map(board -> toSummary(board, userMap, clubMap, gameMap))
                .toList();
    }

    private ChatRoomBoardSummary toSummary(ChatBoardInfo board,
                                           Map<Long, ChatUserInfo> userMap,
                                           Map<Long, ChatClubInfo> clubMap,
                                           Map<Long, ChatGameInfo> gameMap) {
        ChatUserInfo user = board.userId() != null ? userMap.get(board.userId()) : null;
        ChatClubInfo userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        ChatClubInfo cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        ChatGameInfo game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        ChatClubInfo homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        ChatClubInfo awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return ChatRoomBoardSummary.from(board, false, user, userClub, cheerClub, game, homeClub, awayClub);
    }
}
