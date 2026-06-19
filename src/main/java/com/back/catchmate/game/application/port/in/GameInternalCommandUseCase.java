package com.back.catchmate.game.application.port.in;

import com.back.catchmate.game.application.dto.command.GameUpsertCommand;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;

public interface GameInternalCommandUseCase {
    GameInternalResponse findOrCreateGame(GameUpsertCommand command);

    GameInternalResponse savePartialGame(GameUpsertCommand command);
}
