package com.back.catchmate.game.domain.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.game.application.port.out.GameRepository;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;

    public Game findOrCreateGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        return gameRepository.findByHomeClubIdAndAwayClubIdAndGameStartDate(homeClubId, awayClubId, gameStartDate)
                .orElseGet(() -> {
                    Game newGame = Game.createGame(homeClubId, awayClubId, gameStartDate, location);
                    return gameRepository.save(newGame);
                });
    }

    // 부분적인 게임 정보로 게임 생성 및 저장 (임시 저장용)
    public Game savePartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
        Game partialGame = Game.builder()
                .gameStartDate(gameStartDate)
                .location(location)
                .homeClubId(homeClubId)
                .awayClubId(awayClubId)
                .build();
        return gameRepository.save(partialGame);
    }

    public Game getGame(Long gameId) {
        if (gameId == null) {
            return null;
        }
        return gameRepository.findById(gameId)
                .orElseThrow(() -> new BaseException(ErrorCode.GAME_NOT_FOUND));
    }

    public List<Game> getGames(List<Long> gameIds) {
        return gameRepository.findAllByIds(gameIds);
    }
}
