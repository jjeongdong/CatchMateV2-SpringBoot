package com.back.catchmate.enroll.adapter.out.external;

import com.back.catchmate.board.application.dto.command.BoardUpdateCommand;
import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardUpdateResponse;
import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.enroll.application.port.out.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EnrollBoardFetchAdapter implements BoardFetchPort {
    private final BoardService boardService;

    @Override
    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        return boardService.getBoard(userId, boardId);
    }

    @Override
    public Board getBoard(Long boardId) {
        return boardService.getBoard(boardId);
    }

    @Override
    public Board getBoardWithLock(Long boardId) {
        return boardService.getBoardWithLock(boardId);
    }

    @Override
    public Board getCompletedBoard(Long boardId) {
        return boardService.getCompletedBoard(boardId);
    }

    @Override
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        return boardService.updateBoard(userId, boardId, command);
    }

    @Override
    public void updateBoard(Board board) {
        boardService.updateBoard(board);
    }
}
