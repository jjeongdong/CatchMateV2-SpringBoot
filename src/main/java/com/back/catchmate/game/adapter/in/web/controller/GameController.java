package com.back.catchmate.game.adapter.in.web.controller;

import com.back.catchmate.game.application.dto.response.GameResponse;
import com.back.catchmate.game.application.port.in.GameClientQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "[사용자] 경기 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/games")
public class GameController {
    private final GameClientQueryUseCase gameClientQueryUseCase;

    @GetMapping
    @Operation(summary = "경기 목록 조회 API",
            description = "글 작성 시 직관할 경기를 선택하기 위한 목록을 조회합니다. gameDate(경기 날짜), clubId(홈/원정 구단)로 필터링할 수 있습니다.")
    public ResponseEntity<List<GameResponse>> getGameList(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gameDate,
            @RequestParam(required = false) Long clubId) {
        return ResponseEntity.ok(gameClientQueryUseCase.getGameList(gameDate, clubId));
    }
}
