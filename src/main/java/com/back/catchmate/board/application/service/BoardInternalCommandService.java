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
        // 낙관적 락(@Version): 동시 수락 시 이 save 의 커밋에서 버전 충돌이 감지되고,
        // 신청 수락 진입점(EnrollAcceptExecutor)의 @Retryable 이 재시도한다.
        Board board = boardReader.getBoard(boardId);
        board.increaseCurrentPerson();
        boardRepository.save(board);
    }
}
