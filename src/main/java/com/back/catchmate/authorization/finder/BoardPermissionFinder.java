package com.back.catchmate.authorization.finder;

import com.back.catchmate.application.board.service.BoardService;
import com.back.catchmate.authorization.common.DomainFinder;
import com.back.catchmate.domain.board.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardPermissionFinder implements DomainFinder<Board> {
    private final BoardService boardService;

    @Override
    public Board searchById(Long boardId) {
        return boardService.getBoard(boardId);
    }
}
