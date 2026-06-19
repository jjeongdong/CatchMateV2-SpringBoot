package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.port.out.persistence.BoardRepository;
import com.back.catchmate.board.domain.dto.BoardSearchCondition;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.CursorPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BoardReader {
    private final BoardRepository boardRepository;

    public Board getBoard(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new BaseException(ErrorCode.BOARD_NOT_FOUND));
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

    public CursorPage<Board> getBoardListByCondition(BoardSearchCondition condition, int size) {
        return boardRepository.findAllByConditionWithCursor(condition, size);
    }

    public long getTotalBoardCount() {
        return boardRepository.count();
    }
}
