package com.back.catchmate.board.application.service;

import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.application.port.out.BoardRepository;
import com.back.catchmate.common.page.CursorPage;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BoardService {
    private final BoardRepository boardRepository;

    public Board createBoard(Board board) {
        return boardRepository.save(board);
    }

    public Board getBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
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

    public DomainPage<Board> getBoardList(DomainPageable pageable) {
        return boardRepository.findAll(pageable);
    }

    public DomainPage<Board> getBoardList(BoardSearchCondition condition, DomainPageable pageable) {
        return boardRepository.findAllByCondition(condition, pageable);
    }

    public CursorPage<Board> getBoardListWithCursor(BoardSearchCondition condition, int size) {
        return boardRepository.findAllByConditionWithCursor(condition, size);
    }

    public DomainPage<Board> getBoardListByUserId(Long userId, DomainPageable pageable) {
        return boardRepository.findAllByUserId(userId, pageable);
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
}
