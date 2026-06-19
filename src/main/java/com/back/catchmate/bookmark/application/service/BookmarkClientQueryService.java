package com.back.catchmate.bookmark.application.service;

import com.back.catchmate.bookmark.application.dto.response.BookmarkedBoardSummary;
import com.back.catchmate.bookmark.application.port.in.BookmarkClientQueryUseCase;
import com.back.catchmate.bookmark.application.port.out.external.BoardFetchPort;
import com.back.catchmate.bookmark.application.port.out.external.ClubFetchPort;
import com.back.catchmate.bookmark.application.port.out.external.GameFetchPort;
import com.back.catchmate.bookmark.application.port.out.external.UserFetchPort;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkBoardInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkClubInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkGameInfo;
import com.back.catchmate.bookmark.application.port.out.dto.BookmarkUserInfo;
import com.back.catchmate.bookmark.domain.model.Bookmark;
import com.back.catchmate.common.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkClientQueryService implements BookmarkClientQueryUseCase {
    private final BookmarkReader bookmarkReader;
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;
    private final BoardFetchPort boardFetchPort;

    @Override
    public PagedResponse<BookmarkedBoardSummary> getBookmarkedBoards(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Bookmark> bookmarkPage = bookmarkReader.findAllByUserId(userId, pageable);

        if (bookmarkPage.isEmpty()) {
            return new PagedResponse<>(bookmarkPage, List.of());
        }

        List<Long> boardIds = bookmarkPage.getContent().stream()
                .map(Bookmark::getBoardId)
                .toList();

        List<BookmarkBoardInfo> boards = boardFetchPort.getBoards(boardIds);
        List<BookmarkedBoardSummary> responses = assembleSummaries(boards);
        return new PagedResponse<>(bookmarkPage, responses);
    }

    private List<BookmarkedBoardSummary> assembleSummaries(List<BookmarkBoardInfo> boards) {
        if (boards.isEmpty()) return List.of();

        List<Long> userIds = boards.stream()
                .map(BookmarkBoardInfo::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(BookmarkBoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, BookmarkUserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userFetchPort.getUsers(userIds).stream()
                        .collect(Collectors.toMap(BookmarkUserInfo::userId, Function.identity()));
        Map<Long, BookmarkGameInfo> gameMap = gameIds.isEmpty() ? Map.of() :
                gameFetchPort.getGames(gameIds).stream()
                        .collect(Collectors.toMap(BookmarkGameInfo::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(BookmarkBoardInfo::cheerClubId),
                        gameMap.values().stream().map(BookmarkGameInfo::homeClubId),
                        gameMap.values().stream().map(BookmarkGameInfo::awayClubId),
                        userMap.values().stream().map(BookmarkUserInfo::clubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, BookmarkClubInfo> clubMap = clubIds.isEmpty() ? Map.of() :
                clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(BookmarkClubInfo::clubId, Function.identity()));

        return boards.stream()
                .map(board -> toSummary(board, userMap, clubMap, gameMap))
                .toList();
    }

    private BookmarkedBoardSummary toSummary(BookmarkBoardInfo board, Map<Long, BookmarkUserInfo> userMap, Map<Long, BookmarkClubInfo> clubMap, Map<Long, BookmarkGameInfo> gameMap) {
        BookmarkUserInfo user = board.userId() != null ? userMap.get(board.userId()) : null;
        BookmarkClubInfo userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        BookmarkClubInfo cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        BookmarkGameInfo game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        BookmarkClubInfo homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        BookmarkClubInfo awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return BookmarkedBoardSummary.from(board, true, user, userClub, cheerClub, game, homeClub, awayClub);
    }
}
