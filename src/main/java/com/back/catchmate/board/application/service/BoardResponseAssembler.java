package com.back.catchmate.board.application.service;

import com.back.catchmate.board.application.dto.response.BoardDetailResponse;
import com.back.catchmate.board.application.dto.response.BoardResponse;
import com.back.catchmate.board.application.dto.response.BoardTempDetailResponse;
import com.back.catchmate.board.application.port.out.dto.BoardClubInfo;
import com.back.catchmate.board.application.port.out.dto.BoardGameInfo;
import com.back.catchmate.board.application.port.out.dto.BoardUserInfo;
import com.back.catchmate.board.application.port.out.external.ClubFetchPort;
import com.back.catchmate.board.application.port.out.external.GameFetchPort;
import com.back.catchmate.board.application.port.out.external.UserFetchPort;
import com.back.catchmate.board.domain.model.Board;
import com.back.catchmate.board.domain.model.BoardButtonStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class BoardResponseAssembler {
    private final UserFetchPort userFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;

    public BoardResponse buildBoardResponse(Board board, boolean bookmarked) {
        BoardReferences references = loadReferences(List.of(board));
        return toBoardResponse(board, bookmarked, references);
    }

    public List<BoardResponse> buildBoardResponses(List<Board> boards, Predicate<Long> bookmarkedPredicate) {
        if (boards.isEmpty()) return List.of();
        BoardReferences references = loadReferences(boards);
        return boards.stream()
                .map(board -> toBoardResponse(board, bookmarkedPredicate.test(board.getId()), references))
                .toList();
    }

    public BoardDetailResponse buildBoardDetailResponse(Board board, boolean bookMarked, BoardButtonStatus buttonStatus, Long myEnrollId, Long chatRoomId) {
        BoardReferences references = loadReferences(List.of(board));
        BoardUserInfo user = references.user(board);
        BoardGameInfo game = references.game(board);

        return BoardDetailResponse.of(
                board,
                bookMarked,
                buttonStatus,
                myEnrollId,
                chatRoomId,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game)
        );
    }

    public BoardTempDetailResponse buildTempDetailResponse(Board board) {
        BoardReferences references = loadReferences(List.of(board));
        BoardUserInfo user = references.user(board);
        BoardGameInfo game = references.game(board);
        return BoardTempDetailResponse.from(
                board,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game)
        );
    }

    private BoardReferences loadReferences(Collection<Board> boards) {
        List<Long> userIds = boards.stream()
                .map(Board::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Long> gameIds = boards.stream()
                .map(Board::getGameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, BoardUserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userFetchPort.getUsers(userIds).stream()
                        .collect(Collectors.toMap(BoardUserInfo::userId, Function.identity()));

        Map<Long, BoardGameInfo> gameMap = gameIds.isEmpty() ? Map.of() :
                gameFetchPort.getGames(gameIds).stream()
                        .collect(Collectors.toMap(BoardGameInfo::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(Board::getCheerClubId),
                        gameMap.values().stream().map(BoardGameInfo::homeClubId),
                        gameMap.values().stream().map(BoardGameInfo::awayClubId),
                        userMap.values().stream().map(BoardUserInfo::clubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, BoardClubInfo> clubMap = clubIds.isEmpty() ? Map.of() :
                clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(BoardClubInfo::clubId, Function.identity()));

        return new BoardReferences(userMap, clubMap, gameMap);
    }

    private BoardResponse toBoardResponse(Board board, boolean bookMarked, BoardReferences references) {
        BoardUserInfo user = references.user(board);
        BoardGameInfo game = references.game(board);
        return BoardResponse.from(
                board,
                bookMarked,
                user,
                references.userClub(user),
                references.cheerClub(board),
                game,
                references.homeClub(game),
                references.awayClub(game)
        );
    }

    private record BoardReferences(
            Map<Long, BoardUserInfo> userMap,
            Map<Long, BoardClubInfo> clubMap,
            Map<Long, BoardGameInfo> gameMap
    ) {
        private BoardUserInfo user(Board board) {
            return board.getUserId() != null ? userMap.get(board.getUserId()) : null;
        }

        private BoardClubInfo userClub(BoardUserInfo user) {
            return user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        }

        private BoardClubInfo cheerClub(Board board) {
            return board.getCheerClubId() != null ? clubMap.get(board.getCheerClubId()) : null;
        }

        private BoardGameInfo game(Board board) {
            return board.getGameId() != null ? gameMap.get(board.getGameId()) : null;
        }

        private BoardClubInfo homeClub(BoardGameInfo game) {
            return game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        }

        private BoardClubInfo awayClub(BoardGameInfo game) {
            return game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        }
    }
}
