package com.back.catchmate.board.service;

import com.back.catchmate.board.dto.request.BoardSearchCondition;
import com.back.catchmate.board.dto.response.BoardDetailResponse;
import com.back.catchmate.board.dto.response.BoardResponse;
import com.back.catchmate.board.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.entity.Board;
import com.back.catchmate.board.entity.BoardButtonStatus;
import com.back.catchmate.bookmark.service.BookmarkService;
import com.back.catchmate.chat.service.ChatQueryService;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.CursorPage;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.dto.response.EnrollSummary;
import com.back.catchmate.enroll.service.EnrollQueryService;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.BlockService;
import com.back.catchmate.user.service.UserService;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardResponseAssembler {
    private final BoardService boardService;

    // 조회자 관점 조립을 위해 클러스터 서비스를 여기서 주입한다.
    // BoardService 는 이들을 주입하지 않는다 — 되부르면 생성자 주입 순환이다.
    private final BookmarkService bookmarkService;
    private final EnrollQueryService enrollQueryService;
    private final ChatQueryService chatQueryService;

    private final UserService userService;
    private final BlockService blockService;
    private final ClubService clubService;
    private final GameService gameService;

    public BoardResponse buildBoardResponse(Board board, boolean bookmarked) {
        BoardReferences references = loadReferences(List.of(board));
        return toBoardResponse(board, bookmarked, references);
    }

    public List<BoardResponse> buildBoardResponses(List<Board> boards, Predicate<Long> bookmarkedPredicate) {
        if (boards.isEmpty()) return List.of();
        BoardReferences references = loadReferences(boards);
        return boards.stream()
                .map(board -> toBoardResponse(board, bookmarkedPredicate.test(board.getId()), references))
                .toList();
    }

    public BoardDetailResponse buildBoardDetailResponse(
            Board board, boolean bookMarked, BoardButtonStatus buttonStatus, Long myEnrollId, Long chatRoomId) {
        BoardReferences references = loadReferences(List.of(board));
        UserSummary user = references.user(board);
        GameSummary game = references.game(board);

        return BoardDetailResponse.of(
                board,
                bookMarked,
                buttonStatus,
                myEnrollId,
                chatRoomId,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game));
    }

    public BoardTempDetailResponse buildTempDetailResponse(Board board) {
        BoardReferences references = loadReferences(List.of(board));
        UserSummary user = references.user(board);
        GameSummary game = references.game(board);
        return BoardTempDetailResponse.from(
                board,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game));
    }

    private BoardReferences loadReferences(Collection<Board> boards) {
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

        Map<Long, UserSummary> userMap = userIds.isEmpty()
                ? Map.of()
                : userService.getUserSummaries(userIds).stream()
                        .collect(Collectors.toMap(UserSummary::userId, Function.identity()));

        Map<Long, GameSummary> gameMap = gameIds.isEmpty()
                ? Map.of()
                : gameService.getGameSummaries(gameIds).stream()
                        .collect(Collectors.toMap(GameSummary::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(Board::getCheerClubId),
                        gameMap.values().stream().map(GameSummary::homeClubId),
                        gameMap.values().stream().map(GameSummary::awayClubId),
                        userMap.values().stream().map(UserSummary::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ClubSummary> clubMap = clubIds.isEmpty()
                ? Map.of()
                : clubService.getClubSummaries(clubIds).stream()
                        .collect(Collectors.toMap(ClubSummary::clubId, Function.identity()));

        return new BoardReferences(userMap, clubMap, gameMap);
    }

    private BoardResponse toBoardResponse(Board board, boolean bookMarked, BoardReferences references) {
        UserSummary user = references.user(board);
        GameSummary game = references.game(board);
        return BoardResponse.from(
                board,
                bookMarked,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game));
    }

    private record BoardReferences(
            Map<Long, UserSummary> userMap, Map<Long, ClubSummary> clubMap, Map<Long, GameSummary> gameMap) {
        private UserSummary user(Board board) {
            return board.getUserId() != null ? userMap.get(board.getUserId()) : null;
        }

        private ClubSummary userClub(UserSummary user) {
            return user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        }

        private ClubSummary cheerClub(Board board) {
            return board.getCheerClubId() != null ? clubMap.get(board.getCheerClubId()) : null;
        }

        private GameSummary game(Board board) {
            return board.getGameId() != null ? gameMap.get(board.getGameId()) : null;
        }

        private ClubSummary homeClub(GameSummary game) {
            return game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        }

        private ClubSummary awayClub(GameSummary game) {
            return game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        }
    }

    // ── 조회 진입점 (컨트롤러가 부른다) ────────────────────────────────
    // ⚠️ 이 클래스에는 @Transactional 을 걸지 않는다. 여러 서비스를 순차 호출하므로
    //    조립기에 트랜잭션을 걸면 한 커넥션을 그 시간 내내 점유한다.
    //    각 서비스가 자기 트랜잭션을 열고 닫는다.

    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);
        boolean isBookMarked = bookmarkService.isBookmarked(userId, boardId);
        Optional<EnrollSummary> myEnroll = enrollQueryService.findEnrollByUserIdAndBoardId(userId, boardId);

        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(
                userId, board, myEnroll.map(EnrollSummary::acceptStatus).orElse(null));
        Long myEnrollId = myEnroll.map(EnrollSummary::enrollId).orElse(null);
        Long chatRoomId = findChatRoomId(board);

        return buildBoardDetailResponse(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId);
    }

    public CursorPagedResponse<BoardResponse> getBoardList(
            Long userId,
            LocalDate gameDate,
            Integer maxPerson,
            List<Long> preferredTeamIdList,
            LocalDateTime lastLiftUpDate,
            Long lastBoardId,
            int size) {
        List<Long> blockedUserIds = blockService.getBlockedUserIds(userId);
        List<Long> matchingGameIds = gameDate != null ? gameService.findIdsByGameStartDateOn(gameDate) : null;

        // 날짜 필터를 줬는데 매칭 경기가 0건이면 게시글도 없음 — 짧은 회로
        if (matchingGameIds != null && matchingGameIds.isEmpty()) {
            return new CursorPagedResponse<>(new CursorPage<>(List.of(), false, null, null), List.of());
        }

        BoardSearchCondition condition = BoardSearchCondition.of(
                userId, matchingGameIds, maxPerson, preferredTeamIdList, blockedUserIds, lastLiftUpDate, lastBoardId);

        CursorPage<Board> boardPage = boardService.getBoardListByCondition(condition, size);
        List<Board> boards = boardPage.getContent();

        if (boards.isEmpty()) {
            return new CursorPagedResponse<>(boardPage, List.of());
        }

        Set<Long> myBookmarkedBoardIds = findBookmarkedBoardIds(userId, boards);
        List<BoardResponse> boardResponses = buildBoardResponses(boards, myBookmarkedBoardIds::contains);

        return new CursorPagedResponse<>(boardPage, boardResponses);
    }

    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        UserSummary targetUser = userService.getUserSummary(targetUserId);
        UserSummary loginUser = userService.getUserSummary(loginUserId);

        if (blockService.isUserBlocked(targetUser.userId(), loginUser.userId())) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        Page<Board> boardPage = boardService.getBoardListByUserId(targetUserId, page, size);
        List<Board> boards = boardPage.getContent();
        Set<Long> myBookmarkedBoardIds = findBookmarkedBoardIds(loginUser.userId(), boards);
        List<BoardResponse> responses = buildBoardResponses(boards, myBookmarkedBoardIds::contains);

        return new PagedResponse<>(boardPage, responses);
    }

    public BoardTempDetailResponse getTempBoard(Long userId) {
        return boardService
                .findTempBoard(userId)
                .map(this::buildTempDetailResponse)
                .orElse(null);
    }

    private Set<Long> findBookmarkedBoardIds(Long userId, List<Board> boards) {
        List<Long> boardIds = boards.stream().map(Board::getId).toList();
        if (boardIds.isEmpty()) return Set.of();
        return new HashSet<>(bookmarkService.findBookmarkedBoardIds(userId, boardIds));
    }

    private Long findChatRoomId(Board board) {
        if (!board.isCompleted()) return null;
        return chatQueryService.findChatRoomIdByBoardId(board.getId()).orElse(null);
    }
}
