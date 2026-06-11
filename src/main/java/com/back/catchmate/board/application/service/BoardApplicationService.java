package com.back.catchmate.board.application.service;


import com.back.catchmate.board.application.port.in.BoardUseCase;
import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.bookmark.application.service.BookmarkService;
import com.back.catchmate.chat.application.service.ChatRoomMemberService;
import com.back.catchmate.chat.application.service.ChatRoomService;
import com.back.catchmate.club.application.service.ClubService;
import com.back.catchmate.enroll.application.service.EnrollService;
import com.back.catchmate.user.application.service.BlockService;
import com.back.catchmate.user.application.service.UserService;
import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import com.back.catchmate.board.domain.model.PreferredAgeRange;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.page.CursorPage;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.game.domain.service.GameService;
import com.back.catchmate.user.domain.model.User;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.chat.application.event.ChatRoomMemberJoinedEvent;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.board.application.dto.command.BoardCreateCommand;
import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.command.GameCreateCommand;
import com.back.catchmate.board.application.dto.command.GameUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardCreateResponse;
import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.common.orchestration.CursorPagedResponse;
import com.back.catchmate.common.orchestration.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardApplicationService implements BoardUseCase {
    private final ClubService clubService;
    private final GameService gameService;
    private final UserService userService;
    private final BlockService blockService;
    private final BoardService boardService;
    private final EnrollService enrollService;
    private final BookmarkService bookmarkService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        // 기존 임시저장 글 삭제
        Optional<Board> oldDraft = boardService.findTempBoard(userId);
        oldDraft.ifPresent(boardService::deleteBoardHard);

        User user = userService.getUser(userId);
        Club cheerClub = getCheerClub(command.getCheerClubId());
        Game game = getGame(command.getGameCreateCommand());

        // 도메인 객체 생성
        Board board = Board.createBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                user,
                cheerClub,
                game,
                command.getPreferredGender(),
                PreferredAgeRange.of(command.getPreferredAgeRange()),
                command.isCompleted()
        );

        Board savedBoard = boardService.createBoard(board);

        // 채팅방 생성 및 멤버 추가
        if (command.isCompleted()) {
            ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(savedBoard);
            chatRoomMemberService.addMember(chatRoom, user);
            applicationEventPublisher.publishEvent(ChatRoomMemberJoinedEvent.of(chatRoom.getId(), user));
        }

        return BoardCreateResponse.of(savedBoard.getId());
    }

    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        User user = userService.getUser(userId);
        Board board = boardService.getBoard(boardId);
        boolean isBookMarked = bookmarkService.isBookmarked(userId, boardId);

        Optional<Enroll> myEnroll = enrollService.findEnrollByUserAndBoard(user, board);
        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(user, board, myEnroll);
        Long myEnrollId = myEnroll.map(Enroll::getId).orElse(null);

        Long chatRoomId = board.isCompleted()
                ? chatRoomService.getOrCreateChatRoom(board).getId()
                : null;

        return BoardDetailResponse.from(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId);
    }

    public CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size) {
        User user = userService.getUser(userId);
        List<Long> blockedUserIds = blockService.getBlockedUserIds(user);

        BoardSearchCondition condition = BoardSearchCondition.ofCursor(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                blockedUserIds,
                lastLiftUpDate,
                lastBoardId
        );

        CursorPage<Board> boardPage = boardService.getBoardListWithCursor(condition, size);

        List<Long> boardIds = boardPage.getContent().stream()
                .map(Board::getId)
                .toList();

        Set<Long> myBookmarkedBoardIds = new HashSet<>(
                bookmarkService.findBookmarkedBoardIds(user, boardIds)
        );

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> BoardResponse.from(board, myBookmarkedBoardIds.contains(board.getId())))
                .toList();

        return new CursorPagedResponse<>(boardPage, boardResponses);
    }

    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        User targetUser = userService.getUser(targetUserId);
        User loginUser = userService.getUser(loginUserId);

        if (blockService.isUserBlocked(targetUser, loginUser)) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        DomainPageable domainPageable = DomainPageable.of(page, size);
        DomainPage<Board> boardPage = boardService.getBoardListByUserId(targetUserId, domainPageable);

        List<Long> boardIds = boardPage.getContent().stream()
                .map(Board::getId)
                .toList();

        Set<Long> myBookmarkedBoardIds = new HashSet<>(
                bookmarkService.findBookmarkedBoardIds(loginUser, boardIds)
        );

        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> BoardResponse.from(board, myBookmarkedBoardIds.contains(board.getId())))
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public BoardTempDetailResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = boardService.findTempBoard(userId);
        return tempBoard.map(BoardTempDetailResponse::from).orElse(null);
    }

    @Transactional
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = boardService.getBoard(boardId);
        User user = userService.getUser(userId);

        boolean wasCompleted = board.isCompleted();
        Club cheerClub = getCheerClub(command.getCheerClubId());
        Game game = getGame(command.getGameUpdateCommand());

        board.updateBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                cheerClub,
                game,
                command.getPreferredGender(),
                PreferredAgeRange.of(command.getPreferredAgeRange()),
                command.isCompleted()
        );

        boardService.updateBoard(board);

        if (!wasCompleted && command.isCompleted()) {
            ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(board);
            chatRoomMemberService.addMember(chatRoom, user);
            applicationEventPublisher.publishEvent(ChatRoomMemberJoinedEvent.of(chatRoom.getId(), user));
        }

        return BoardUpdateResponse.of(board.getId());
    }

    @Transactional
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);

        if (board.canLiftUp()) {
            board.updateLiftUpDate(LocalDateTime.now());
            boardService.updateBoard(board);
            return BoardLiftUpResponse.of(true, null);
        }

        long remainingMinutes = board.getRemainingMinutesForLiftUp();
        return BoardLiftUpResponse.fromRemainingMinutes(false, remainingMinutes);
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);
        board.delete();

        boardService.updateBoard(board);
    }

    // --- Private Helper Methods ---
    private Club getCheerClub(Long clubId) {
        if (clubId == null) return null;
        return clubService.getClub(clubId);
    }

    private Game getGame(GameCreateCommand command) {
        if (command == null) return null;
        return resolveGame(command.getHomeClubId(), command.getAwayClubId(), command.getGameStartDate(), command.getLocation());
    }

    private Game getGame(GameUpdateCommand command) {
        if (command == null) return null;
        return resolveGame(command.getHomeClubId(), command.getAwayClubId(), command.getGameStartDate(), command.getLocation());
    }

    private Game resolveGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        if (gameStartDate == null && homeClubId == null && awayClubId == null && location == null) {
            return null;
        }
        if (homeClubId != null && awayClubId != null && gameStartDate != null) {
            return fetchGame(homeClubId, awayClubId, gameStartDate, location);
        }
        return createPartialGame(gameStartDate, location, homeClubId, awayClubId);
    }

    private Game fetchGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        Club homeClub = clubService.getClub(homeClubId);
        Club awayClub = clubService.getClub(awayClubId);
        return gameService.findOrCreateGame(homeClub, awayClub, gameStartDate, location);
    }

    private Game createPartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
        Club homeClub = homeClubId != null ? clubService.getClub(homeClubId) : null;
        Club awayClub = awayClubId != null ? clubService.getClub(awayClubId) : null;
        return gameService.savePartialGame(gameStartDate, location, homeClub, awayClub);
    }

}
