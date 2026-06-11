package com.back.catchmate.global.authorization.finder;

import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.global.authorization.common.DomainFinder;
import com.back.catchmate.board.domain.model.Board;
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
