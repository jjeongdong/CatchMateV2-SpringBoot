package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.port.in.BoardInternalCommandUseCase;
import com.back.catchmate.board.application.port.out.persistence.BoardRepository;
import com.back.catchmate.board.domain.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class BoardInternalCommandService implements BoardInternalCommandUseCase {
    private final BoardRepository boardRepository;
    private final BoardReader boardReader;

    @Override
    public void increaseCurrentPerson(Long boardId) {
        Board board = boardReader.getBoardWithLock(boardId);
        board.increaseCurrentPerson();
        boardRepository.save(board);
    }
}
