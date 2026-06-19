package com.back.catchmate.game.application.service;

import com.back.catchmate.game.application.dto.command.GameUpsertCommand;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalCommandUseCase;
import com.back.catchmate.game.application.port.out.persistence.GameRepository;
import com.back.catchmate.game.domain.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class GameInternalCommandService implements GameInternalCommandUseCase {
    private final GameRepository gameRepository;

    @Override
    public GameInternalResponse findOrCreateGame(GameUpsertCommand command) {
        Game game = gameRepository.findByHomeClubIdAndAwayClubIdAndGameStartDate(
                        command.homeClubId(), command.awayClubId(), command.gameStartDate())
                .orElseGet(() -> {
                    Game newGame = Game.createGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
                    return gameRepository.save(newGame);
                });
        return toInternalResponse(game);
    }

    @Override
    public GameInternalResponse savePartialGame(GameUpsertCommand command) {
        Game partialGame = Game.createGame(command.homeClubId(), command.awayClubId(), command.gameStartDate(), command.location());
        return toInternalResponse(gameRepository.save(partialGame));
    }

    private GameInternalResponse toInternalResponse(Game game) {
        if (game == null) return null;
        return new GameInternalResponse(
                game.getId(),
                game.getGameStartDate(),
                game.getLocation(),
                game.getHomeClubId(),
                game.getAwayClubId()
        );
    }
}
