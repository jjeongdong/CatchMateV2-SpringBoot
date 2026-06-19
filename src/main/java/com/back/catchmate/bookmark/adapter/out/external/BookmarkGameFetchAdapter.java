package com.back.catchmate.bookmark.adapter.out.external;

import com.back.catchmate.bookmark.application.port.out.external.GameFetchPort;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkGameInfo;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BookmarkGameFetchAdapter implements GameFetchPort {
    private final GameInternalQueryUseCase gameInternalQueryUseCase;

    @Override
    public List<BookmarkGameInfo> getGames(List<Long> gameIds) {
        List<GameInternalResponse> games = gameInternalQueryUseCase.getGames(gameIds);

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
