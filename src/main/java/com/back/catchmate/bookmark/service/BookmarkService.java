package com.back.catchmate.bookmark.service;

import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.bookmark.dto.response.BookmarkUpdateResponse;
import com.back.catchmate.bookmark.dto.response.BookmarkedBoardSummary;
import com.back.catchmate.bookmark.entity.Bookmark;
import com.back.catchmate.bookmark.repository.BookmarkRepository;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookmarkService {
    private final BookmarkRepository bookmarkRepository;

    private final ClubQueryApi clubQueryApi;
    private final GameQueryApi gameQueryApi;
    private final UserQueryApi userQueryApi;
    // board 는 아직 헥사고날이라 정문 UseCase 로 진입한다 (board 전환 시 BoardService 로 교체)
    private final BoardService boardService;

    @Transactional
    public BookmarkUpdateResponse updateBookmark(Long userId, Long boardId) {
        Optional<Bookmark> bookmarkOptional = bookmarkRepository.findByUserIdAndBoardId(userId, boardId);

        if (bookmarkOptional.isPresent()) {
            bookmarkRepository.deleteById(bookmarkOptional.get().getId());
            return BookmarkUpdateResponse.of(boardId, false);
        }

        bookmarkRepository.save(Bookmark.createBookmark(userId, boardId));
        return BookmarkUpdateResponse.of(boardId, true);
    }

    public PagedResponse<BookmarkedBoardSummary> getBookmarkedBoards(Long userId, int page, int size) {
        Page<Bookmark> bookmarkPage =
                bookmarkRepository.findAllByUserId(userId, sortByCreatedAtDesc(PageRequest.of(page, size)));

        if (bookmarkPage.isEmpty()) {
            return new PagedResponse<>(bookmarkPage, List.of());
        }

        List<Long> boardIds =
                bookmarkPage.getContent().stream().map(Bookmark::getBoardId).toList();

        List<BoardSummary> boards = boardService.getBoardSummaries(boardIds);
        List<BookmarkedBoardSummary> responses = assembleSummaries(boards);
        return new PagedResponse<>(bookmarkPage, responses);
    }

    // 다른 컨텍스트용
    public boolean isBookmarked(Long userId, Long boardId) {
        return bookmarkRepository.existsByUserIdAndBoardId(userId, boardId);
    }

    // 다른 컨텍스트용
    public List<Long> findBookmarkedBoardIds(Long userId, List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) {
            return List.of();
        }
        return bookmarkRepository.findBookmarkedBoardIds(userId, boardIds);
    }

    // 목록 조회는 호출자가 넘긴 정렬을 무시하고 항상 최신순으로 고정한다.
    // (기존 BookmarkRepositoryImpl.findAllByUserId 동작을 그대로 옮긴 것)
    private PageRequest sortByCreatedAtDesc(Pageable pageable) {
        return PageRequest.of(
                pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private List<BookmarkedBoardSummary> assembleSummaries(List<BoardSummary> boards) {
        if (boards.isEmpty()) return List.of();

        List<Long> userIds = boards.stream()
                .map(BoardSummary::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(BoardSummary::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, UserInfo> userMap = userIds.isEmpty() ? Map.of() : userQueryApi.getInfos(userIds);
        Map<Long, GameInfo> gameMap = gameIds.isEmpty() ? Map.of() : gameQueryApi.getInfos(gameIds);

        List<Long> clubIds = Stream.of(
                        boards.stream().map(BoardSummary::cheerClubId),
                        gameMap.values().stream().map(GameInfo::homeClubId),
                        gameMap.values().stream().map(GameInfo::awayClubId),
                        userMap.values().stream().map(UserInfo::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, ClubInfo> clubMap = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);

        return boards.stream()
                .map(board -> toSummary(board, userMap, clubMap, gameMap))
                .toList();
    }

    private BookmarkedBoardSummary toSummary(
            BoardSummary board, Map<Long, UserInfo> userMap, Map<Long, ClubInfo> clubMap, Map<Long, GameInfo> gameMap) {
        UserInfo user = board.userId() != null ? userMap.get(board.userId()) : null;
        ClubInfo userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        ClubInfo cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        GameInfo game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        ClubInfo homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        ClubInfo awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return BookmarkedBoardSummary.from(board, true, user, userClub, cheerClub, game, homeClub, awayClub);
    }
}
