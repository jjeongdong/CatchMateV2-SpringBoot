package com.back.catchmate.bookmark.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult.ClubView;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult.GameView;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult.UserView;
import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.bookmark.domain.BookmarkRepository;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkQueryService {
    private final BookmarkRepository bookmarkRepository;
    private final BoardQueryApi boardQueryApi;
    private final UserQueryApi userQueryApi;
    private final GameQueryApi gameQueryApi;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public OffsetPageResult<BookmarkedBoardResult> getBookmarks(Long userId, int page, int size) {
        List<Bookmark> bookmarks = bookmarkRepository.findAllByUserId(userId, (long) page * size, size);
        long totalElements = bookmarkRepository.countByUserId(userId);
        if (bookmarks.isEmpty()) {
            return OffsetPageResult.of(List.of(), page, size, totalElements);
        }
        Map<Long, BoardInfo> boardById = boardQueryApi.getInfos(
                bookmarks.stream().map(Bookmark::getBoardId).toList());
        // 찜한 순서를 지키고, 그 사이 삭제된 게시글은 뺀다.
        List<BoardInfo> boards = bookmarks.stream()
                .map(bookmark -> boardById.get(bookmark.getBoardId()))
                .filter(Objects::nonNull)
                .toList();
        return OffsetPageResult.of(toResults(boards), page, size, totalElements);
    }

    // 작성자·경기·구단을 BC 마다 한 번씩만 조회한다.
    private List<BookmarkedBoardResult> toResults(List<BoardInfo> boards) {
        if (boards.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = boards.stream().map(BoardInfo::userId).distinct().toList();
        List<Long> gameIds = boards.stream()
                .map(BoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserInfo> userById = userQueryApi.getInfos(userIds);
        Map<Long, GameInfo> gameById = gameIds.isEmpty() ? Map.of() : gameQueryApi.getInfos(gameIds);
        List<Long> clubIds = Stream.of(
                        boards.stream().map(BoardInfo::cheerClubId),
                        gameById.values().stream().map(GameInfo::homeClubId),
                        gameById.values().stream().map(GameInfo::awayClubId),
                        userById.values().stream().map(UserInfo::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
        return boards.stream()
                .map(board -> {
                    UserInfo user = userById.get(board.userId());
                    GameInfo game = board.gameId() != null ? gameById.get(board.gameId()) : null;
                    return BookmarkedBoardResult.of(
                            board,
                            UserView.of(user, user != null ? club(clubById, user.clubId()) : null),
                            ClubView.from(club(clubById, board.cheerClubId())),
                            game == null
                                    ? null
                                    : GameView.of(
                                            game,
                                            club(clubById, game.homeClubId()),
                                            club(clubById, game.awayClubId())));
                })
                .toList();
    }

    private static ClubInfo club(Map<Long, ClubInfo> clubById, Long clubId) {
        return clubId != null ? clubById.get(clubId) : null;
    }
}
