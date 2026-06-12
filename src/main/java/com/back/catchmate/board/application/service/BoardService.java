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
import com.back.catchmate.common.response.CursorPage;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.game.domain.model.Game;
import com.back.catchmate.user.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        Game game = resolveGame(command.gameCreateCommand());
        Long cheerClubId = command.cheerClubId();

        Board board = Board.createBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                user.getId(),
                cheerClubId,
                game != null ? game.getId() : null,
                game != null && game.isComplete(),
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

        Optional<Enroll> myEnroll = enrollFetchPort.findEnrollByUserIdAndBoardId(user.getId(), board.getId());
        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(user.getId(), board, myEnroll.map(Enroll::getAcceptStatus));
        Long myEnrollId = myEnroll.map(Enroll::getId).orElse(null);

        Long chatRoomId = board.isCompleted()
                ? chatRoomFetchPort.getOrCreateChatRoom(board.getId()).getId()
                : null;

        User boardOwner = userFetchPort.getUser(board.getUserId());
        Club ownerClub = boardOwner.getClubId() != null ? clubFetchPort.getClub(boardOwner.getClubId()) : null;
        Club cheerClub = board.getCheerClubId() != null ? clubFetchPort.getClub(board.getCheerClubId()) : null;
        Game game = board.getGameId() != null ? gameFetchPort.getGame(board.getGameId()) : null;
        Club homeClub = game != null && game.getHomeClubId() != null ? clubFetchPort.getClub(game.getHomeClubId()) : null;
        Club awayClub = game != null && game.getAwayClubId() != null ? clubFetchPort.getClub(game.getAwayClubId()) : null;

        return BoardDetailResponse.from(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId,
                boardOwner, ownerClub, cheerClub, game, homeClub, awayClub);
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

        BoardRefs refs = loadRefs(boardPage.getContent());

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> toBoardResponse(board, myBookmarkedBoardIds.contains(board.getId()), refs))
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

        BoardRefs refs = loadRefs(boardPage.getContent());

        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> toBoardResponse(board, myBookmarkedBoardIds.contains(board.getId()), refs))
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    @Override
    public BoardTempDetailResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = findTempBoard(userId);
        return tempBoard.map(board -> {
            User owner = board.getUserId() != null ? userFetchPort.getUser(board.getUserId()) : null;
            Club ownerClub = owner != null && owner.getClubId() != null ? clubFetchPort.getClub(owner.getClubId()) : null;
            Club cheerClub = board.getCheerClubId() != null ? clubFetchPort.getClub(board.getCheerClubId()) : null;
            Game game = board.getGameId() != null ? gameFetchPort.getGame(board.getGameId()) : null;
            Club homeClub = game != null && game.getHomeClubId() != null ? clubFetchPort.getClub(game.getHomeClubId()) : null;
            Club awayClub = game != null && game.getAwayClubId() != null ? clubFetchPort.getClub(game.getAwayClubId()) : null;
            return BoardTempDetailResponse.from(board, owner, ownerClub, cheerClub, game, homeClub, awayClub);
        }).orElse(null);
    }

    @Override
    @Transactional
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = getBoardEntity(boardId);
        User user = userFetchPort.getUser(userId);

        boolean wasCompleted = board.isCompleted();
        Game game = resolveGame(command.gameUpdateCommand());
        Long cheerClubId = command.cheerClubId();

        board.updateBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                cheerClubId,
                game != null ? game.getId() : null,
                game != null && game.isComplete(),
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
    private Game resolveGame(GameCreateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
    }

    private Game resolveGame(GameUpdateCommand command) {
        if (command == null) return null;
        return resolveGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
    }

    private Game resolveGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        if (gameStartDate == null && homeClubId == null && awayClubId == null && location == null) {
            return null;
        }
        if (homeClubId != null && awayClubId != null && gameStartDate != null) {
            return gameFetchPort.findOrCreateGame(homeClubId, awayClubId, gameStartDate, location);
        }
        return gameFetchPort.savePartialGame(gameStartDate, location, homeClubId, awayClubId);
    }

    // --- Cross-context response builders (used by adapters of other contexts) ---
    public BoardResponse buildBoardResponse(Board board, boolean bookmarked) {
        BoardRefs refs = loadRefs(List.of(board));
        return toBoardResponse(board, bookmarked, refs);
    }

    public List<BoardResponse> buildBoardResponses(List<Board> boards, java.util.function.Predicate<Long> bookmarkedPredicate) {
        if (boards.isEmpty()) return List.of();
        BoardRefs refs = loadRefs(boards);
        return boards.stream()
                .map(b -> toBoardResponse(b, bookmarkedPredicate.test(b.getId()), refs))
                .toList();
    }

    // --- Bulk reference loading for list responses ---
    private record BoardRefs(
            Map<Long, User> userById,
            Map<Long, Club> clubById,
            Map<Long, Game> gameById
    ) {}

    private BoardRefs loadRefs(Collection<Board> boards) {
        List<Long> userIds = boards.stream()
                .map(Board::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(Board::getGameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, User> userById = userIds.isEmpty()
                ? Map.of()
                : userFetchPort.getUsers(userIds).stream()
                        .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, Game> gameById = gameIds.isEmpty()
                ? Map.of()
                : gameFetchPort.getGames(gameIds).stream()
                        .collect(Collectors.toMap(Game::getId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(Board::getCheerClubId),
                        gameById.values().stream().map(Game::getHomeClubId),
                        gameById.values().stream().map(Game::getAwayClubId),
                        userById.values().stream().map(User::getClubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Club> clubById = clubIds.isEmpty()
                ? Map.of()
                : clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(Club::getId, Function.identity()));

        return new BoardRefs(userById, clubById, gameById);
    }

    private BoardResponse toBoardResponse(Board board, boolean bookMarked, BoardRefs refs) {
        User user = board.getUserId() != null ? refs.userById().get(board.getUserId()) : null;
        Club userClub = user != null && user.getClubId() != null ? refs.clubById().get(user.getClubId()) : null;
        Club cheerClub = board.getCheerClubId() != null ? refs.clubById().get(board.getCheerClubId()) : null;
        Game game = board.getGameId() != null ? refs.gameById().get(board.getGameId()) : null;
        Club homeClub = game != null && game.getHomeClubId() != null ? refs.clubById().get(game.getHomeClubId()) : null;
        Club awayClub = game != null && game.getAwayClubId() != null ? refs.clubById().get(game.getAwayClubId()) : null;
        return BoardResponse.from(board, bookMarked, user, userClub, cheerClub, game, homeClub, awayClub);
    }
}
