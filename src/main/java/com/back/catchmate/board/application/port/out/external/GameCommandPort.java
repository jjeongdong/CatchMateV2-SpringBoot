package com.back.catchmate.board.application.port.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.dto.BoardGameUpsertCommand;

public interface GameCommandPort {
    BoardGameInfo findOrCreateGame(BoardGameUpsertCommand command);

    BoardGameInfo savePartialGame(BoardGameUpsertCommand command);
}
