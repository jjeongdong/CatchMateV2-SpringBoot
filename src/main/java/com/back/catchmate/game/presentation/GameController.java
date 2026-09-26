package com.back.catchmate.game.presentation;

import com.back.catchmate.game.application.GameQueryService;
import com.back.catchmate.game.application.dto.result.GameResult;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController implements GameApiDocs {
    private final GameQueryService gameQueryService;

    @Override
    @GetMapping
    public ResponseEntity<List<GameResult>> getGames(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate gameDate,
            @RequestParam(required = false) Long clubId) {
        return ResponseEntity.ok(gameQueryService.getGames(gameDate, clubId));
    }
}
