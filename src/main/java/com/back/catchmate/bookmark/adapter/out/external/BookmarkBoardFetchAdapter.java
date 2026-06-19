package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.board.application.dto.response.BoardInternalResponse;
import com.back.catchmate.board.application.port.in.BoardInternalQueryUseCase;
import com.back.catchmate.bookmark.application.port.out.external.BoardFetchPort;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkBoardInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkBoardFetchAdapter implements BoardFetchPort {
    private final BoardInternalQueryUseCase boardInternalQueryUseCase;

    @Override
    public List<BookmarkBoardInfo> getBoards(List<Long> boardIds) {
        List<BoardInternalResponse> boards = boardInternalQueryUseCase.getBoards(boardIds);

        return boards.stream()
                .map(board -> new BookmarkBoardInfo(
                        board.boardId(),
                        board.userId(),
                        board.gameId(),
                        board.cheerClubId(),
                        board.title(),
                        board.content(),
                        board.currentPerson(),
                        board.maxPerson()
                ))
                .collect(Collectors.toList());
    }
}
