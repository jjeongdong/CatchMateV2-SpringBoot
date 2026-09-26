package com.back.catchmate.game.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.global.error.ErrorCode;
import com.back.catchmate.global.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameNotFoundExceptionTest {

    @Test
    @DisplayName("기존과 같은 코드·메시지의 NOT_FOUND 에러를 담는다")
    void carriesGameNotFoundCode() {
        // when
        ErrorCode errorCode = new GameNotFoundException().getErrorCode();

        // then
        assertThat(errorCode.name()).isEqualTo("GAME_NOT_FOUND");
        assertThat(errorCode.type()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(errorCode.message()).isEqualTo("존재하지 않는 게임입니다.");
    }
}
