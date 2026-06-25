package com.back.catchmate.game.application.port.in;

import com.back.catchmate.game.application.dto.response.GameResponse;

import java.time.LocalDate;
import java.util.List;

public interface GameClientQueryUseCase {
    /**
     * 글 작성 화면에서 직관할 경기를 선택하기 위한 목록 조회.
     * gameDate / clubId 는 선택값으로 동적 필터링한다.
     */
    List<GameResponse> getGameList(LocalDate gameDate, Long clubId);
}
