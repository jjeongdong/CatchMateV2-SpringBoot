package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.external.GameFetchPort;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkGameInfo;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkGameFetchAdapter implements GameFetchPort {
    private final GameService gameService;

    @Override
    public List<BookmarkGameInfo> getGames(List<Long> gameIds) {
        List<GameSummary> games = gameService.getGameSummaries(gameIds);

        return games.stream()
                .map(game -> new BookmarkGameInfo(
                        game.gameId(),
                        game.homeClubId(),
                        game.awayClubId(),
                        game.location()
                ))
                .collect(Collectors.toList());
    }
}
