package com.back.catchmate.game.domain;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;

public enum GameErrorCode implements ErrorCode {
    GAME_NOT_FOUND(ErrorType.NOT_FOUND, "존재하지 않는 게임입니다.");

    private final ErrorType type;
    private final String message;

    GameErrorCode(ErrorType type, String message) {
        this.type = type;
        this.message = message;
    }

    @Override
    public ErrorType type() {
        return type;
    }

    @Override
    public String message() {
        return message;
    }
}
