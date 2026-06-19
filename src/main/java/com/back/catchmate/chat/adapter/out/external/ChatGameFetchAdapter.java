package com.back.catchmate.chat.adapter.out.external;

import com.back.catchmate.chat.application.port.out.dto.ChatGameInfo;
import com.back.catchmate.chat.application.port.out.external.GameFetchPort;
import com.back.catchmate.game.application.dto.response.GameInternalResponse;
import com.back.catchmate.game.application.port.in.GameInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChatGameFetchAdapter implements GameFetchPort {
    private final GameInternalQueryUseCase gameInternalQueryUseCase;

    @Override
    public List<ChatGameInfo> getGames(List<Long> gameIds) {
        List<GameInternalResponse> responses = gameInternalQueryUseCase.getGames(gameIds);
        return responses.stream()
                .map(response -> new ChatGameInfo(
                        response.gameId(),
                        response.gameStartDate(),
                        response.homeClubId(),
                        response.awayClubId(),
                        response.location()
                ))
                .collect(Collectors.toList());
    }
}
