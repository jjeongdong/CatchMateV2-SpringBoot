package com.back.catchmate.board.application;

import com.back.catchmate.board.application.dto.command.BoardSearchCommand;
import com.back.catchmate.board.application.dto.result.AdminBoardDetailResult;
import com.back.catchmate.board.application.dto.result.AdminBoardDetailResult.EnrollmentView;
import com.back.catchmate.board.application.dto.result.AdminBoardResult;
import com.back.catchmate.board.application.dto.result.BoardDetailResult;
import com.back.catchmate.board.application.dto.result.BoardDraftResult;
import com.back.catchmate.board.application.dto.result.BoardResult;
import com.back.catchmate.board.application.dto.result.BoardResult.ClubView;
import com.back.catchmate.board.application.dto.result.BoardResult.GameView;
import com.back.catchmate.board.application.dto.result.BoardResult.WriterView;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardButtonStatus;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.BoardSearchCondition;
import com.back.catchmate.board.domain.exception.BoardWriterBlockedException;
import com.back.catchmate.bookmark.application.BookmarkQueryApi;
import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.EnrollQueryApi;
import com.back.catchmate.enroll.application.dto.api.EnrollInfo;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardQueryService {
    private final BoardRepository boardRepository;
    private final UserQueryApi userQueryApi;
    private final GameQueryApi gameQueryApi;
    private final ClubQueryApi clubQueryApi;
    private final EnrollQueryApi enrollQueryApi;
    private final BookmarkQueryApi bookmarkQueryApi;
    private final ChatQueryApi chatQueryApi;

    @Transactional(readOnly = true)
    public BoardDetailResult getBoard(Long userId, Long boardId) {
        Board board = boardRepository.getById(boardId);
        boolean bookmarked = bookmarkQueryApi.isBookmarked(userId, boardId);
        Optional<EnrollInfo> myEnroll = enrollQueryApi.findInfo(userId, boardId);
        BoardButtonStatus buttonStatus = BoardButtonStatus.resolve(
                userId,
                board.getUserId(),
                myEnroll.map(EnrollInfo::acceptStatus).orElse(null));
        // 채팅방은 발행 시점에 만들어지므로 임시저장 글에는 없다.
        Long chatRoomId = board.isCompleted()
                ? chatQueryApi.findChatRoomIdByBoardId(boardId).orElse(null)
                : null;
        References references = loadReferences(List.of(board));
        return BoardDetailResult.of(
                board,
                bookmarked,
                buttonStatus,
                myEnroll.map(EnrollInfo::enrollId).orElse(null),
                chatRoomId,
                references.writer(board),
                references.cheerClub(board),
                references.game(board));
    }

    @Transactional(readOnly = true)
    public Optional<BoardDraftResult> getMyDraft(Long userId) {
        return boardRepository.findDraftByWriterId(userId).map(draft -> {
            References references = loadReferences(List.of(draft));
            return BoardDraftResult.of(
                    draft, references.writer(draft), references.cheerClub(draft), references.game(draft));
        });
    }

    @Transactional(readOnly = true)
    public CursorPageResult<BoardResult> getBoards(Long userId, BoardSearchCommand command) {
        BoardCursor cursor = decode(command.cursor());
        List<Long> blockedUserIds = userQueryApi.getBlockedUserIds(userId);
        List<Long> matchingGameIds =
                command.gameDate() != null ? gameQueryApi.getIdsStartingOn(command.gameDate()) : null;
        // 날짜 필터를 줬는데 그날 경기가 없으면 게시글도 없다.
        if (matchingGameIds != null && matchingGameIds.isEmpty()) {
            return CursorPageResult.empty();
        }
        BoardSearchCondition condition = new BoardSearchCondition(
                matchingGameIds,
                command.maxPerson(),
                command.preferredTeamIds(),
                blockedUserIds,
                null,
                cursor != null ? cursor.liftUpDate() : null,
                cursor != null ? cursor.boardId() : null);
        return toCursorPage(userId, boardRepository.findAllByCondition(condition, command.size() + 1), command.size());
    }

    @Transactional(readOnly = true)
    public CursorPageResult<BoardResult> getUserBoards(Long loginUserId, Long writerId, String cursor, int size) {
        BoardCursor decoded = decode(cursor);
        userQueryApi.getInfo(writerId); // 없는 사용자면 404
        // 타 BC 값(차단 여부)만으로 판단해 담을 엔티티가 없어 여기서 검사한다 (spec §8).
        if (userQueryApi.isBlocked(loginUserId, writerId)) {
            throw new BoardWriterBlockedException();
        }
        BoardSearchCondition condition = new BoardSearchCondition(
                null,
                null,
                null,
                null,
                writerId,
                decoded != null ? decoded.liftUpDate() : null,
                decoded != null ? decoded.boardId() : null);
        return toCursorPage(loginUserId, boardRepository.findAllByCondition(condition, size + 1), size);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<AdminBoardResult> getAdminBoards(Long userId, int page, int size) {
        long offset = (long) page * size;
        List<Board> boards = userId == null
                ? boardRepository.findAllPublished(offset, size)
                : boardRepository.findAllByWriterId(userId, offset, size);
        long totalElements =
                userId == null ? boardRepository.countPublished() : boardRepository.countByWriterId(userId);
        return OffsetPageResult.of(boards.stream().map(AdminBoardResult::from).toList(), page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public AdminBoardDetailResult getAdminBoard(Long boardId) {
        Board board = boardRepository.getPublishedById(boardId);
        List<EnrollInfo> enrolls = enrollQueryApi.getPendingInfosByBoardId(boardId);
        List<Long> userIds = Stream.concat(
                        Stream.of(board.getUserId()), enrolls.stream().map(EnrollInfo::userId))
                .distinct()
                .toList();
        Map<Long, UserInfo> userById = userQueryApi.getInfos(userIds);
        List<Long> clubIds = enrolls.stream()
                .map(enroll -> userById.get(enroll.userId()))
                .filter(Objects::nonNull)
                .map(UserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
        GameInfo game = board.getGameId() != null ? gameQueryApi.getInfo(board.getGameId()) : null;
        List<EnrollmentView> enrollments = enrolls.stream()
                .map(enroll -> {
                    UserInfo user = userById.get(enroll.userId());
                    ClubInfo club = user != null && user.clubId() != null ? clubById.get(user.clubId()) : null;
                    return EnrollmentView.of(enroll, user, club);
                })
                .toList();
        return AdminBoardDetailResult.of(board, userById.get(board.getUserId()), game, enrollments);
    }

    // 첫 페이지 요청에 빈 cursor 파라미터를 붙이는 클라이언트도 있어 공백은 커서 없음으로 본다.
    private static BoardCursor decode(String cursor) {
        return cursor == null || cursor.isBlank() ? null : BoardCursor.decode(cursor);
    }

    // limit = size + 1 로 읽은 결과에서 다음 페이지 유무를 판단한다.
    private CursorPageResult<BoardResult> toCursorPage(Long viewerId, List<Board> fetched, int size) {
        boolean hasNext = fetched.size() > size;
        List<Board> boards = hasNext ? fetched.subList(0, size) : fetched;
        if (boards.isEmpty()) {
            return CursorPageResult.empty();
        }
        Set<Long> bookmarkedIds = bookmarkQueryApi.getBookmarkedBoardIds(
                viewerId, boards.stream().map(Board::getId).toList());
        References references = loadReferences(boards);
        List<BoardResult> content = boards.stream()
                .map(board -> BoardResult.of(
                        board,
                        bookmarkedIds.contains(board.getId()),
                        references.writer(board),
                        references.cheerClub(board),
                        references.game(board)))
                .toList();
        Board last = boards.get(boards.size() - 1);
        String nextCursor = hasNext ? new BoardCursor(last.getLiftUpDate(), last.getId()).encode() : null;
        return new CursorPageResult<>(content, nextCursor, hasNext);
    }

    // 작성자·경기·구단을 BC 마다 한 번씩만 조회한다.
    private References loadReferences(Collection<Board> boards) {
        List<Long> userIds = boards.stream().map(Board::getUserId).distinct().toList();
        List<Long> gameIds = boards.stream()
                .map(Board::getGameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, UserInfo> userById = userQueryApi.getInfos(userIds);
        Map<Long, GameInfo> gameById = gameIds.isEmpty() ? Map.of() : gameQueryApi.getInfos(gameIds);
        List<Long> clubIds = Stream.of(
                        boards.stream().map(Board::getCheerClubId),
                        gameById.values().stream().map(GameInfo::homeClubId),
                        gameById.values().stream().map(GameInfo::awayClubId),
                        userById.values().stream().map(UserInfo::clubId))
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubInfo> clubById = clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
        return new References(userById, clubById, gameById);
    }

    private record References(
            Map<Long, UserInfo> userById, Map<Long, ClubInfo> clubById, Map<Long, GameInfo> gameById) {
        WriterView writer(Board board) {
            UserInfo user = userById.get(board.getUserId());
            return WriterView.of(user, user != null ? club(user.clubId()) : null);
        }

        ClubView cheerClub(Board board) {
            return ClubView.from(club(board.getCheerClubId()));
        }

        GameView game(Board board) {
            GameInfo game = board.getGameId() != null ? gameById.get(board.getGameId()) : null;
            return game == null ? null : GameView.of(game, club(game.homeClubId()), club(game.awayClubId()));
        }

        private ClubInfo club(Long clubId) {
            return clubId != null ? clubById.get(clubId) : null;
        }
    }
}
