package com.back.catchmate.board.service;

import com.back.catchmate.board.dto.command.BoardCreateCommand;
import com.back.catchmate.board.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.dto.request.BoardSearchCondition;
import com.back.catchmate.board.dto.response.BoardAdminView;
import com.back.catchmate.board.dto.response.BoardCreateResponse;
import com.back.catchmate.board.dto.response.BoardLiftUpResponse;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.entity.Board;
import com.back.catchmate.board.entity.PreferredAgeRange;
import com.back.catchmate.board.event.BoardCompletedEvent;
import com.back.catchmate.board.repository.BoardRepository;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.CursorPage;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * board 데이터만 다룬다. bookmark·chat·enroll 은 주입하지 않는다 — 그쪽 서비스들이 이 서비스를
 * 주입하므로, 여기서 되부르면 생성자 주입 순환이 되어 Spring 부팅이 깨진다.
 * 조회자 관점 조립(찜 여부·내 신청·채팅방 id)은 {@link BoardResponseAssembler} 가 담당한다.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final GameQueryApi gameQueryApi;

    // ── 쓰기 ──────────────────────────────────────────────────────────
    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        findTempBoard(userId).ifPresent(this::deleteTempBoard);

        GameInfo game = resolveGame(command.gameId());

        Board board = Board.createBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                userId,
                command.cheerClubId(),
                game != null ? game.gameId() : null,
                game != null && isGameComplete(game),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed());

        Board savedBoard = boardRepository.save(board);

        if (command.completed()) {
            applicationEventPublisher.publishEvent(BoardCompletedEvent.of(savedBoard.getId(), userId));
        }

        return BoardCreateResponse.from(savedBoard);
    }

    @Transactional
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = getBoard(boardId);
        verifyBoardOwner(board, userId);
        boolean wasCompleted = board.isCompleted();
        GameInfo game = resolveGame(command.gameId());

        board.updateBoard(
                command.title(),
                command.content(),
                command.maxPerson(),
                command.cheerClubId(),
                game != null ? game.gameId() : null,
                game != null && isGameComplete(game),
                command.preferredGender(),
                PreferredAgeRange.of(command.preferredAgeRange()),
                command.completed());

        boardRepository.save(board);

        if (!wasCompleted && command.completed()) {
            applicationEventPublisher.publishEvent(BoardCompletedEvent.of(board.getId(), userId));
        }

        return BoardUpdateResponse.from(board);
    }

    @Transactional
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = getBoard(boardId);
        verifyBoardOwner(board, userId);

        if (!board.canLiftUp()) {
            long remainingMinutes = board.getRemainingMinutesForLiftUp();
            return BoardLiftUpResponse.fromRemainingMinutes(false, remainingMinutes);
        }

        board.updateLiftUpDate(LocalDateTime.now());
        boardRepository.save(board);
        return BoardLiftUpResponse.of(true, null);
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        Board board = getBoard(boardId);
        verifyBoardOwner(board, userId);
        board.delete(); // 완성 게시글: soft delete (deletedAt 세팅)
        boardRepository.save(board);
    }

    // 다른 컨텍스트용 — enroll 수락 시 인원 증가 (낙관적 락 @Version 충돌 지점)
    @Transactional
    public void increaseCurrentPerson(Long boardId) {
        Board board = getBoard(boardId);
        board.increaseCurrentPerson();
        boardRepository.save(board);
    }

    // ── 자기 컨텍스트 조회 (조립기가 쓴다) ─────────────────────────────
    public Board getBoard(Long boardId) {
        return boardRepository.findById(boardId).orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }

    public List<Board> getBoards(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) return List.of();
        return boardRepository.findAllById(boardIds);
    }

    public Board getCompletedBoard(Long boardId) {
        return boardRepository
                .findByIdAndCompletedTrue(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
    }

    public Optional<Board> findTempBoard(Long userId) {
        return boardRepository.findFirstByUserIdAndCompletedFalse(userId);
    }

    public Page<Board> getBoardListByUserId(Long userId, int page, int size) {
        // 사용자별 목록은 끌어올린 순서가 기준이다 (전체 목록의 createdAt 기준과 다르다)
        return boardRepository.findAllByUserId(
                userId, PageRequest.of(page, size, Sort.by("liftUpDate").descending()));
    }

    public CursorPage<Board> getBoardListByCondition(BoardSearchCondition condition, int size) {
        return boardRepository.findAllByConditionWithCursor(condition, size);
    }

    // ── 다른 컨텍스트용 요약 ───────────────────────────────────────────
    public BoardSummary getBoardSummary(Long boardId) {
        return toSummary(getBoard(boardId));
    }

    public List<BoardSummary> getBoardSummaries(List<Long> boardIds) {
        return getBoards(boardIds).stream().map(this::toSummary).toList();
    }

    public BoardSummary getCompletedBoardSummary(Long boardId) {
        return toSummary(getCompletedBoard(boardId));
    }

    public Page<BoardAdminView> getBoardAdminViews(Pageable pageable) {
        // 관리자 전체 목록은 최신 등록순으로 고정한다
        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        return boardRepository.findAllByCompletedTrue(sorted).map(this::toAdminView);
    }

    public Page<BoardAdminView> getBoardAdminViewsByUserId(Long userId, Pageable pageable) {
        PageRequest sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("liftUpDate").descending());
        return boardRepository.findAllByUserId(userId, sorted).map(this::toAdminView);
    }

    public long getTotalBoardCount() {
        return boardRepository.count();
    }

    // ── 내부 ──────────────────────────────────────────────────────────
    private void deleteTempBoard(Board board) {
        // draft(미완성)는 일회성·고빈도라 물리 삭제 (soft-delete 누적 방지). 의도된 예외.
        boardRepository.deleteById(board.getId()); // arch-audit:allow-hard-delete
    }

    private void verifyBoardOwner(Board board, Long userId) {
        if (!board.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
    }

    // 게시글 발행에 충분한 경기 정보인지 판정한다 (board 의 규칙 — 전환 전 BoardGameInfo.isComplete()).
    private boolean isGameComplete(GameInfo game) {
        return game.homeClubId() != null
                && game.awayClubId() != null
                && game.gameStartDate() != null
                && game.location() != null;
    }

    private GameInfo resolveGame(Long gameId) {
        if (gameId == null) {
            return null;
        }
        return gameQueryApi.getInfo(gameId);
    }

    private BoardSummary toSummary(Board board) {
        return new BoardSummary(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.getUserId(),
                board.getCheerClubId(),
                board.getGameId(),
                board.getPreferredGender(),
                board.getPreferredAgeRange() != null
                        ? board.getPreferredAgeRange().asList()
                        : List.of(),
                board.isCompleted(),
                board.getCreatedAt(),
                board.getLiftUpDate());
    }

    private BoardAdminView toAdminView(Board board) {
        return new BoardAdminView(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                board.getMaxPerson(),
                board.getCurrentPerson(),
                board.getUserId(),
                board.getGameId(),
                board.isCompleted(),
                board.getCreatedAt());
    }
}
