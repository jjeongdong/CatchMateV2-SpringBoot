package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import com.back.catchmate.chat.application.port.out.dto.ChatBoardInfo;
import com.back.catchmate.chat.application.port.out.external.BoardFetchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatBoardFetchAdapter implements BoardFetchPort {
    private final BoardInternalQueryUseCase boardInternalQueryUseCase;

    @Override
    public ChatBoardInfo getBoard(Long boardId) {
        return fromInternalResponse(boardInternalQueryUseCase.getBoard(boardId));
    }

    @Override
    public List<ChatBoardInfo> getBoards(List<Long> boardIds) {
        return boardInternalQueryUseCase.getBoards(boardIds).stream()
                .map(this::fromInternalResponse)
                .toList();
    }

    private ChatBoardInfo fromInternalResponse(BoardInternalResponse response) {
        if (response == null) return null;
        return new ChatBoardInfo(
                response.boardId(),
                response.title(),
                response.content(),
                response.currentPerson(),
                response.maxPerson() != null ? response.maxPerson() : 0,
                response.userId(),
                response.cheerClubId(),
                response.gameId()
        );
    }
}
