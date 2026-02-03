package com.back.catchmate.domain.game.service;

import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    public Game findOrCreateGame(Club homeClub, Club awayClub, LocalDateTime gameStartDate, String location) {
        return gameRepository.findByHomeClubAndAwayClubAndGameStartDate(homeClub, awayClub, gameStartDate)
                .orElseGet(() -> {
                    Game newGame = Game.createGame(homeClub, awayClub, gameStartDate, location);
                    return gameRepository.save(newGame);
                });
    }

    // 부분적인 게임 정보로 게임 생성 및 저장 (임시 저장용)
    public Game savePartialGame(LocalDateTime gameStartDate, String location, Club homeClub, Club awayClub) {
        Game partialGame = Game.builder()
                .gameStartDate(gameStartDate)
                .location(location)
                .homeClub(homeClub)
                .awayClub(awayClub)
                .build();
        return gameRepository.save(partialGame);
    }
}
