package com.back.catchmate.game.presentation;

import com.back.catchmate.game.application.dto.result.GameResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "[사용자] 경기 관련 API")
public interface GameApiDocs {

    @Operation(
            summary = "경기 목록 조회 API",
            description = "글 작성 시 직관할 경기를 선택하기 위한 목록을 조회합니다. gameDate(경기 날짜), clubId(홈/원정 구단)로 필터링할 수 있습니다.")
    ResponseEntity<List<GameResult>> getGames(LocalDate gameDate, Long clubId);
}
