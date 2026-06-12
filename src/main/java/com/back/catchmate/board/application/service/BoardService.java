package com.back.catchmate.board.application.service;

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
import com.back.catchmate.board.application.port.in.BoardUseCase;
import com.back.catchmate.board.application.port.out.BlockFetchPort;
import com.back.catchmate.board.application.port.out.BoardRepository;
import com.back.catchmate.board.application.port.out.BookmarkFetchPort;
import com.back.catchmate.board.application.port.out.ChatRoomFetchPort;
import com.back.catchmate.board.application.port.out.ClubFetchPort;
import com.back.catchmate.board.application.port.out.EnrollFetchPort;
import com.back.catchmate.board.application.port.out.GameFetchPort;
import com.back.catchmate.board.application.port.out.UserFetchPort;
import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import com.back.catchmate.board.domain.model.PreferredAgeRange;
import com.back.catchmate.chat.application.event.ChatRoomMemberJoinedEvent;
import com.back.catchmate.chat.domain.model.ChatRoom;
import com.back.catchmate.club.domain.model.Club;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.common.response.CursorPage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService implements BoardUseCase {

    private final BoardRepository boardRepository;

    private final BlockFetchPort blockFetchPort;
    private final BookmarkFetchPort bookmarkFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        Optional<Board> oldDraft = findTempBoard(userId);
        oldDraft.ifPresent(this::deleteBoardHard);

        User user = userFetchPort.getUser(userId);
        Club cheerClub = getCheerClub(command.cheerClubId());
        Game game = getGame(command.gameCreateCommand());

        Board board = Board.createBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                user,
                cheerClub,
                game,
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed()
        );

        Board savedBoard = createBoardEntity(board);

        if (command.completed()) {
            ChatRoom chatRoom = chatRoomFetchPort.getOrCreateChatRoom(savedBoard.getId());
            chatRoomFetchPort.addMember(chatRoom, user.getId());
            applicationEventPublisher.publishEvent(ChatRoomMemberJoinedEvent.of(chatRoom.getId(), user));
        }

        return BoardCreateResponse.of(savedBoard.getId());
    }

    @Override
    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        User user = userFetchPort.getUser(userId);
        Board board = getBoardEntity(boardId);
        boolean isBookMarked = bookmarkFetchPort.isBookmarked(userId, boardId);

        Optional<Enroll> myEnroll = enrollFetchPort.findEnrollByUserAndBoard(user, board);
        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(user, board, myEnroll);
        Long myEnrollId = myEnroll.map(Enroll::getId).orElse(null);

        Long chatRoomId = board.isCompleted()
                ? chatRoomFetchPort.getOrCreateChatRoom(board.getId()).getId()
                : null;

        return BoardDetailResponse.from(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId);
    }

    @Override
    public CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                           List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size) {
        User user = userFetchPort.getUser(userId);
        List<Long> blockedUserIds = blockFetchPort.getBlockedUserIds(user.getId());

        BoardSearchCondition condition = BoardSearchCondition.ofCursor(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                blockedUserIds,
                lastLiftUpDate,
                lastBoardId
        );

        CursorPage<Board> boardPage = boardRepository.findAllByConditionWithCursor(condition, size);

        List<Long> boardIds = boardPage.getContent().stream()
                .map(Board::getId)
                .toList();

        Set<Long> myBookmarkedBoardIds = new HashSet<>(
                bookmarkFetchPort.findBookmarkedBoardIds(user.getId(), boardIds)
        );

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> BoardResponse.from(board, myBookmarkedBoardIds.contains(board.getId())))
                .toList();

        return new CursorPagedResponse<>(boardPage, boardResponses);
    }

    @Override
    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        User targetUser = userFetchPort.getUser(targetUserId);
        User loginUser = userFetchPort.getUser(loginUserId);

        if (blockFetchPort.isUserBlocked(targetUser.getId(), loginUser.getId())) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        Pageable domainPageable = PageRequest.of(page, size);
        Page<Board> boardPage = boardRepository.findAllByUserId(targetUserId, domainPageable);

        List<Long> boardIds = boardPage.getContent().stream()
                .map(Board::getId)
                .toList();

        Set<Long> myBookmarkedBoardIds = new HashSet<>(
                bookmarkFetchPort.findBookmarkedBoardIds(loginUser.getId(), boardIds)
        );

        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> BoardResponse.from(board, myBookmarkedBoardIds.contains(board.getId())))
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    @Override
    public BoardTempDetailResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = findTempBoard(userId);
        return tempBoard.map(BoardTempDetailResponse::from).orElse(null);
    }

    @Override
    @Transactional
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = getBoardEntity(boardId);
        User user = userFetchPort.getUser(userId);

        boolean wasCompleted = board.isCompleted();
        Club cheerClub = getCheerClub(command.cheerClubId());
        Game game = getGame(command.gameUpdateCommand());

        board.updateBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                cheerClub,
                game,
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed()
        );

        boardRepository.save(board);

        if (!wasCompleted && command.completed()) {
            ChatRoom chatRoom = chatRoomFetchPort.getOrCreateChatRoom(board.getId());
            chatRoomFetchPort.addMember(chatRoom, user.getId());
            applicationEventPublisher.publishEvent(ChatRoomMemberJoinedEvent.of(chatRoom.getId(), user));
        }

        return BoardUpdateResponse.of(board.getId());
    }

    @Override
    @Transactional
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = getBoardEntity(boardId);

        if (board.canLiftUp()) {
            board.updateLiftUpDate(LocalDateTime.now());
            boardRepository.save(board);
            return BoardLiftUpResponse.of(true, null);
        }

        long remainingMinutes = board.getRemainingMinutesForLiftUp();
        return BoardLiftUpResponse.fromRemainingMinutes(false, remainingMinutes);
    }

    @Override
    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        Board board = getBoardEntity(boardId);
        board.delete();
        boardRepository.save(board);
    }

    // --- Internal / cross-context exposed helpers on own aggregate ---
    public Board createBoardEntity(Board board) {
        return boardRepository.save(board);
    }

    public Board getBoardEntity(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }

    // Kept for cross-context backward compatibility until their Phase B is done.
    public Board getBoard(Long boardId) {
        return getBoardEntity(boardId);
    }

    public List<Board> getBoards(List<Long> boardIds) {
        return boardRepository.findAllByIds(boardIds);
    }

    public Board getBoardWithLock(Long boardId) {
        return boardRepository.findByIdWithLock(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }

    public Board getCompletedBoard(Long boardId) {
        return boardRepository.findCompletedById(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }

    public Optional<Board> findTempBoard(Long userId) {
        return boardRepository.findTempBoardByUserId(userId);
    }

    public Page<Board> getBoardListByUserId(Long userId, Pageable pageable) {
        return boardRepository.findAllByUserId(userId, pageable);
    }

    public Page<Board> getBoardList(Pageable pageable) {
        return boardRepository.findAll(pageable);
    }

    public long getTotalBoardCount() {
        return boardRepository.count();
    }

    public void updateBoard(Board board) {
        boardRepository.save(board);
    }

    public void deleteBoardHard(Board board) {
        boardRepository.delete(board);
    }

    // --- Cross-context game resolution ---
    private Club getCheerClub(Long clubId) {
        if (clubId == null) return null;
        return clubFetchPort.getClub(clubId);
    }

    private Game getGame(GameCreateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
    }

    private Game getGame(GameUpdateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
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
        Club homeClub = clubFetchPort.getClub(homeClubId);
        Club awayClub = clubFetchPort.getClub(awayClubId);
        return gameFetchPort.findOrCreateGame(homeClub, awayClub, gameStartDate, location);
    }

    private Game createPartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
        Club homeClub = homeClubId != null ? clubFetchPort.getClub(homeClubId) : null;
        Club awayClub = awayClubId != null ? clubFetchPort.getClub(awayClubId) : null;
        return gameFetchPort.savePartialGame(gameStartDate, location, homeClub, awayClub);
    }
}
