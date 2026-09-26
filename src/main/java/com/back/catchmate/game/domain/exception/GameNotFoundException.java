package com.back.catchmate.game.domain.exception;

import com.back.catchmate.game.domain.GameErrorCode;
import com.back.catchmate.global.error.BusinessException;

public class GameNotFoundException extends BusinessException {
    public GameNotFoundException() {
        super(GameErrorCode.GAME_NOT_FOUND);
    }
}
