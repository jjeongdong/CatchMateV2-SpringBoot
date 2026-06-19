package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.application.port.in.BoardClientQueryUseCase;
import com.back.catchmate.board.application.port.out.dto.BoardEnrollInfo;
import com.back.catchmate.board.application.port.out.dto.BoardUserInfo;
import com.back.catchmate.board.application.port.out.external.BlockFetchPort;
import com.back.catchmate.board.application.port.out.external.BookmarkFetchPort;
import com.back.catchmate.board.application.port.out.external.ChatRoomFetchPort;
import com.back.catchmate.board.application.port.out.external.EnrollFetchPort;
import com.back.catchmate.board.application.port.out.external.GameFetchPort;
import com.back.catchmate.board.application.port.out.external.UserFetchPort;
import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.CursorPage;
import com.back.catchmate.common.response.CursorPagedResponse;
import com.back.catchmate.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
public class BoardClientQueryService implements BoardClientQueryUseCase {
    private final BoardReader boardReader;
    private final UserFetchPort userFetchPort;
    private final BlockFetchPort blockFetchPort;
    private final EnrollFetchPort enrollFetchPort;
    private final BookmarkFetchPort bookmarkFetchPort;
    private final ChatRoomFetchPort chatRoomFetchPort;
    private final GameFetchPort gameFetchPort;
    private final BoardResponseAssembler boardResponseAssembler;

    @Override
    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        Board board = boardReader.getBoard(boardId);
        boolean isBookMarked = bookmarkFetchPort.isBookmarked(userId, boardId);
        Optional<BoardEnrollInfo> myEnroll = enrollFetchPort.findEnrollByUserIdAndBoardId(userId, boardId);

        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(userId, board, myEnroll.map(BoardEnrollInfo::acceptStatus).orElse(null));
        Long myEnrollId = myEnroll.map(BoardEnrollInfo::enrollId).orElse(null);
        Long chatRoomId = findChatRoomId(board);

        return boardResponseAssembler.buildBoardDetailResponse(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId);
    }

    @Override
    public CursorPagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson, List<Long> preferredTeamIdList,
                                                           LocalDateTime lastLiftUpDate, Long lastBoardId, int size) {
        List<Long> blockedUserIds = blockFetchPort.getBlockedUserIds(userId);
        List<Long> matchingGameIds = gameDate != null ? gameFetchPort.findGameIdsByDate(gameDate) : null;

        // 날짜 필터를 줬는데 매칭 경기가 0건이면 게시글도 없음 — 짧은 회로
        if (matchingGameIds != null && matchingGameIds.isEmpty()) {
            return new CursorPagedResponse<>(new CursorPage<>(List.of(), false, null, null), List.of());
        }

        BoardSearchCondition condition = BoardSearchCondition.of(
                userId, matchingGameIds, maxPerson, preferredTeamIdList, blockedUserIds, lastLiftUpDate, lastBoardId
        );

        CursorPage<Board> boardPage = boardReader.getBoardListByCondition(condition, size);
        List<Board> boards = boardPage.getContent();

        if (boards.isEmpty()) {
            return new CursorPagedResponse<>(boardPage, List.of());
        }

        Set<Long> myBookmarkedBoardIds = findBookmarkedBoardIds(userId, boards);
        List<BoardResponse> boardResponses = boardResponseAssembler.buildBoardResponses(boards, myBookmarkedBoardIds::contains);

        return new CursorPagedResponse<>(boardPage, boardResponses);
    }

    @Override
    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        BoardUserInfo targetUser = userFetchPort.getUser(targetUserId);
        BoardUserInfo loginUser = userFetchPort.getUser(loginUserId);

        if (blockFetchPort.isUserBlocked(targetUser.userId(), loginUser.userId())) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Board> boardPage = boardReader.getBoardListByUserId(targetUserId, pageable);
        List<Board> boards = boardPage.getContent();
        Set<Long> myBookmarkedBoardIds = findBookmarkedBoardIds(loginUser.userId(), boards);
        List<BoardResponse> responses = boardResponseAssembler.buildBoardResponses(boards, myBookmarkedBoardIds::contains);

        return new PagedResponse<>(boardPage, responses);
    }

    @Override
    public BoardTempDetailResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = boardReader.findTempBoard(userId);
        return tempBoard.map(boardResponseAssembler::buildTempDetailResponse).orElse(null);
    }

    private Set<Long> findBookmarkedBoardIds(Long userId, List<Board> boards) {
        List<Long> boardIds = boards.stream().map(Board::getId).toList();
        if (boardIds.isEmpty()) return Set.of();
        return new HashSet<>(bookmarkFetchPort.findBookmarkedBoardIds(userId, boardIds));
    }

    private Long findChatRoomId(Board board) {
        if (!board.isCompleted()) return null;
        return chatRoomFetchPort.findChatRoomIdByBoardId(board.getId()).orElse(null);
    }
}
