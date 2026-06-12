package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.board.application.service.BoardService;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.chat.application.port.out.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatBoardFetchAdapter implements BoardFetchPort {
    private final BoardService boardService;

    @Override
    public Board getBoard(Long boardId) {
        return boardService.getBoard(boardId);
    }

    @Override
    public List<Board> getBoards(List<Long> boardIds) {
        return boardService.getBoards(boardIds);
    }
}
