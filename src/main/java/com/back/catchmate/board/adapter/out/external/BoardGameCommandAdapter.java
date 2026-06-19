package com.back.catchmate.board.adapter.out.external;

import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.dto.BoardGameUpsertCommand;
import com.back.catchmate.board.application.port.out.external.GameCommandPort;
import com.back.catchmate.game.application.dto.command.GameUpsertCommand;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BoardGameCommandAdapter implements GameCommandPort {
    private final GameInternalCommandUseCase gameInternalCommandUseCase;

    @Override
    public BoardGameInfo findOrCreateGame(BoardGameUpsertCommand command) {
        return toBoardGameInfo(gameInternalCommandUseCase.findOrCreateGame(toGameUpsertCommand(command)));
    }

    @Override
    public BoardGameInfo savePartialGame(BoardGameUpsertCommand command) {
        return toBoardGameInfo(gameInternalCommandUseCase.savePartialGame(toGameUpsertCommand(command)));
    }

    private GameUpsertCommand toGameUpsertCommand(BoardGameUpsertCommand command) {
        return new GameUpsertCommand(
                command.homeClubId(),
                command.awayClubId(),
                command.gameStartDate(),
                command.location()
        );
    }

    private BoardGameInfo toBoardGameInfo(GameInternalResponse response) {
        if (response == null) return null;
        return new BoardGameInfo(
                response.gameId(),
                response.gameStartDate(),
                response.location(),
                response.homeClubId(),
                response.awayClubId()
        );
    }
}
