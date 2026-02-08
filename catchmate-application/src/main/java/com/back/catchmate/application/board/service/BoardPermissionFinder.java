package com.back.catchmate.application.board.service;

import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.common.permission.DomainFinder;
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
