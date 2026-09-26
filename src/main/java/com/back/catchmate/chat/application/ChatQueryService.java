package com.back.catchmate.chat.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.chat.application.dto.result.ChatMessageResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomMemberResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult.BoardView;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult.GameView;
import com.back.catchmate.chat.application.dto.result.ChatRoomResult.UserView;
import com.back.catchmate.chat.domain.ChatHistoryPage;
import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatMessageRepository;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ChatRoomMemberRepository;
import com.back.catchmate.chat.domain.ChatRoomRepository;
import com.back.catchmate.chat.domain.exception.ChatCursorInvalidException;
import com.back.catchmate.chat.domain.exception.ChatSubscriptionDestinationInvalidException;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatQueryService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatMembershipReader chatMembershipReader;
    private final ChatReadRecorder chatReadRecorder;
    private final ChatHistoryReader chatHistoryReader;
    private final UserQueryApi userQueryApi;
    private final BoardQueryApi boardQueryApi;
    private final GameQueryApi gameQueryApi;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public OffsetPageResult<ChatRoomResult> getMyChatRooms(Long userId, int page, int size) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllByMemberUserId(userId, (long) page * size, size);
        long totalElements = chatRoomRepository.countByMemberUserId(userId);
        if (chatRooms.isEmpty()) {
            return OffsetPageResult.of(List.of(), page, size, totalElements);
        }
        List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();
        Map<Long, ChatMessage> lastMessageByChatRoomId = chatMessageRepository.findLastTextByChatRoomIds(chatRoomIds);
        Map<Long, ChatRoomMember> myMemberByChatRoomId =
                chatRoomMemberRepository.findActiveByChatRoomIdsAndUserId(chatRoomIds, userId).stream()
                        .collect(Collectors.toMap(
                                member -> member.getChatRoom().getId(), Function.identity(), (first, second) -> first));
        Map<Long, BoardView> boardById = loadBoards(chatRooms);
        Map<Long, UserInfo> senderById = loadSenders(lastMessageByChatRoomId.values());
        List<ChatRoomResult> content = chatRooms.stream()
                .map(chatRoom -> {
                    ChatMessage lastMessage = lastMessageByChatRoomId.get(chatRoom.getId());
                    return ChatRoomResult.of(
                            chatRoom,
                            boardById.get(chatRoom.getBoardId()),
                            lastMessage != null
                                    ? ChatMessageResult.of(lastMessage, senderById.get(lastMessage.getSenderId()))
                                    : null,
                            myMemberByChatRoomId.get(chatRoom.getId()));
                })
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    // 지난 메시지를 불러오면 그 방을 읽음으로 표시한다 (옛 GET 동작 유지, Redis 버퍼만 쓴다).
    @Transactional(readOnly = true)
    public CursorPageResult<ChatMessageResult> getMessages(Long userId, Long chatRoomId, String cursor, int size) {
        chatMembershipReader.get(chatRoomId, userId).verifyActive();
        Long beforeMessageId = parseCursor(cursor);
        chatReadRecorder.record(chatRoomId, userId);
        List<ChatHistoryPage.Entry> entries =
                chatHistoryReader.read(chatRoomId, beforeMessageId, size + 1).messages();
        boolean hasNext = entries.size() > size;
        // 오래된 순이라 초과분은 맨 앞에 있다.
        List<ChatHistoryPage.Entry> page = hasNext ? entries.subList(1, entries.size()) : entries;
        String nextCursor = hasNext ? String.valueOf(page.get(0).id()) : null;
        return new CursorPageResult<>(page.stream().map(ChatMessageResult::from).toList(), nextCursor, hasNext);
    }

    // 재연결 시 놓친 메시지. 한 번에 다 못 받으면 hasNext 로 알려 이어 받게 한다.
    @Transactional(readOnly = true)
    public CursorPageResult<ChatMessageResult> syncMessages(
            Long userId, Long chatRoomId, Long lastMessageId, int size) {
        chatMembershipReader.get(chatRoomId, userId).verifyActive();
        List<ChatMessage> fetched = chatMessageRepository.findAfter(chatRoomId, lastMessageId, size + 1);
        boolean hasNext = fetched.size() > size;
        List<ChatMessage> messages = hasNext ? fetched.subList(0, size) : fetched;
        Map<Long, UserInfo> senderById = loadSenders(messages);
        List<ChatMessageResult> content = messages.stream()
                .map(message -> ChatMessageResult.of(message, senderById.get(message.getSenderId())))
                .toList();
        String nextCursor =
                hasNext ? String.valueOf(messages.get(messages.size() - 1).getId()) : null;
        return new CursorPageResult<>(content, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public Optional<ChatMessageResult> getLastMessage(Long userId, Long chatRoomId) {
        chatMembershipReader.get(chatRoomId, userId).verifyActive();
        return chatMessageRepository
                .findLastText(chatRoomId)
                .map(message -> ChatMessageResult.of(message, userQueryApi.getInfo(message.getSenderId())));
    }

    @Transactional(readOnly = true)
    public List<ChatRoomMemberResult> getChatRoomMembers(Long userId, Long chatRoomId) {
        chatMembershipReader.get(chatRoomId, userId).verifyActive();
        List<ChatRoomMember> members = chatRoomMemberRepository.findActiveByChatRoomId(chatRoomId);
        List<Long> userIds =
                members.stream().map(ChatRoomMember::getUserId).distinct().toList();
        Map<Long, UserInfo> userById = userIds.isEmpty() ? Map.of() : userQueryApi.getInfos(userIds);
        return members.stream()
                .map(member -> ChatRoomMemberResult.of(member, userById.get(member.getUserId())))
                .toList();
    }

    // 구독 권한 검사용. "/sub/chat/room/*" 같은 와일드카드나 숫자가 아닌 주소는 전체 방 구독으로 번질 수 있어 거절한다.
    // 표현 계층은 도메인 예외를 던질 수 없어 주소의 방 번호 부분을 문자열로 받아 여기서 해석한다.
    public void verifySubscription(Long userId, String chatRoomId) {
        Long parsedChatRoomId;
        try {
            parsedChatRoomId = Long.valueOf(chatRoomId);
        } catch (NumberFormatException e) {
            throw new ChatSubscriptionDestinationInvalidException();
        }
        chatMembershipReader.get(parsedChatRoomId, userId).verifyActive();
    }

    private static Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(cursor);
        } catch (NumberFormatException e) {
            throw new ChatCursorInvalidException();
        }
    }

    private Map<Long, UserInfo> loadSenders(Collection<ChatMessage> messages) {
        List<Long> senderIds =
                messages.stream().map(ChatMessage::getSenderId).distinct().toList();
        return senderIds.isEmpty() ? Map.of() : userQueryApi.getInfos(senderIds);
    }

    // 게시글·작성자·경기·구단을 BC 마다 한 번씩만 조회한다.
    private Map<Long, BoardView> loadBoards(List<ChatRoom> chatRooms) {
        List<Long> boardIds =
                chatRooms.stream().map(ChatRoom::getBoardId).distinct().toList();
        Map<Long, BoardInfo> boardById = boardQueryApi.getInfos(boardIds);
        if (boardById.isEmpty()) {
            return Map.of();
        }
        Collection<BoardInfo> boards = boardById.values();
        List<Long> writerIds = boards.stream().map(BoardInfo::userId).distinct().toList();
        List<Long> gameIds = boards.stream()
                .map(BoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserInfo> writerById = userQueryApi.getInfos(writerIds);
        Map<Long, GameInfo> gameById = gameIds.isEmpty() ? Map.of() : gameQueryApi.getInfos(gameIds);
        List<Long> clubIds = Stream.of(
                        boards.stream().map(BoardInfo::cheerClubId),
                        gameById.values().stream().map(GameInfo::homeClubId),
                        gameById.values().stream().map(GameInfo::awayClubId),
                        writerById.values().stream().map(UserInfo::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
        return boards.stream().collect(Collectors.toMap(BoardInfo::boardId, board -> {
            UserInfo writer = writerById.get(board.userId());
            GameInfo game = board.gameId() != null ? gameById.get(board.gameId()) : null;
            return BoardView.of(
                    board,
                    UserView.of(writer, writer != null ? club(clubById, writer.clubId()) : null),
                    club(clubById, board.cheerClubId()),
                    game == null
                            ? null
                            : GameView.of(game, club(clubById, game.homeClubId()), club(clubById, game.awayClubId())));
        }));
    }

    private static ClubInfo club(Map<Long, ClubInfo> clubById, Long clubId) {
        return clubId != null ? clubById.get(clubId) : null;
    }
}
