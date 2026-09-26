package com.back.catchmate.enroll.application;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.bookmark.application.BookmarkQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.enroll.application.dto.result.EnrollApplicantResult;
import com.back.catchmate.enroll.application.dto.result.EnrollBoardResult;
import com.back.catchmate.enroll.application.dto.result.EnrollBoardResult.ClubView;
import com.back.catchmate.enroll.application.dto.result.EnrollBoardResult.GameView;
import com.back.catchmate.enroll.application.dto.result.EnrollBoardResult.WriterView;
import com.back.catchmate.enroll.application.dto.result.EnrollDetailResult;
import com.back.catchmate.enroll.application.dto.result.EnrollPendingCountResult;
import com.back.catchmate.enroll.application.dto.result.EnrollReceivedResult;
import com.back.catchmate.enroll.application.dto.result.EnrollReceivedResult.EnrollView;
import com.back.catchmate.enroll.application.dto.result.EnrollRequestResult;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollQueryService {
    private final EnrollRepository enrollRepository;
    private final BoardQueryApi boardQueryApi;
    private final BookmarkQueryApi bookmarkQueryApi;
    private final UserQueryApi userQueryApi;
    private final GameQueryApi gameQueryApi;
    private final ClubQueryApi clubQueryApi;

    @Transactional(readOnly = true)
    public EnrollDetailResult getEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollRepository.getById(enrollId);
        enroll.verifyParticipant(userId);
        BoardInfo board = boardQueryApi.getInfo(enroll.getBoardId());
        UserInfo applicant = userQueryApi.getInfo(enroll.getUserId());
        ClubInfo applicantClub = applicant.clubId() != null ? clubQueryApi.getInfo(applicant.clubId()) : null;
        // 옛 동작대로 상세의 게시글 찜 여부는 확인하지 않는다 (false).
        EnrollBoardResult boardResult = toBoardResults(List.of(board), Set.of()).get(board.boardId());
        return EnrollDetailResult.of(enroll, WriterView.of(applicant, applicantClub), boardResult);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<EnrollRequestResult> getMyEnrolls(Long userId, int page, int size) {
        List<Enroll> enrolls = enrollRepository.findAllByApplicantId(userId, (long) page * size, size);
        long totalElements = enrollRepository.countByApplicantId(userId);
        if (enrolls.isEmpty()) {
            return OffsetPageResult.of(List.of(), page, size, totalElements);
        }
        List<Long> boardIds =
                enrolls.stream().map(Enroll::getBoardId).distinct().toList();
        Set<Long> bookmarkedIds = bookmarkQueryApi.getBookmarkedBoardIds(userId, boardIds);
        Map<Long, EnrollBoardResult> boardById =
                toBoardResults(boardQueryApi.getInfos(boardIds).values(), bookmarkedIds);
        List<EnrollRequestResult> content = enrolls.stream()
                .map(enroll -> EnrollRequestResult.of(enroll, boardById.get(enroll.getBoardId())))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<EnrollApplicantResult> getBoardEnrolls(Long userId, Long boardId, int page, int size) {
        // 타 BC 값(게시글 작성자)만으로 판단해 담을 엔티티가 없어 여기서 검사한다 (spec §8).
        if (!boardQueryApi.getInfo(boardId).userId().equals(userId)) {
            throw new EnrollNotBoardWriterException();
        }
        List<Enroll> enrolls = enrollRepository.findPendingByBoardId(boardId, (long) page * size, size);
        long totalElements = enrollRepository.countPendingByBoardId(boardId);
        Map<Long, UserInfo> applicantById = findApplicants(enrolls);
        Map<Long, ClubInfo> clubById = findClubs(applicantById.values());
        List<EnrollApplicantResult> content = enrolls.stream()
                .map(enroll -> {
                    UserInfo applicant = applicantById.get(enroll.getUserId());
                    return EnrollApplicantResult.of(enroll, applicant, clubOf(applicant, clubById));
                })
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<EnrollReceivedResult> getReceivedEnrolls(Long userId, int page, int size) {
        List<Long> boardIds = enrollRepository.findBoardIdsWithPendingByOwnerId(userId, (long) page * size, size);
        long totalElements = enrollRepository.countBoardsWithPendingByOwnerId(userId);
        if (boardIds.isEmpty()) {
            return OffsetPageResult.of(List.of(), page, size, totalElements);
        }
        // 페이지 전체를 한 번에 조립한다 — 신청 1회 + 게시글 1회 + (작성자·경기·구단·신청자) 각 1회
        Map<Long, List<Enroll>> enrollsByBoardId = enrollRepository.findPendingByBoardIds(boardIds).stream()
                .collect(Collectors.groupingBy(Enroll::getBoardId));
        Map<Long, EnrollBoardResult> boardById =
                toBoardResults(boardQueryApi.getInfos(boardIds).values(), Set.of());
        Map<Long, UserInfo> applicantById = findApplicants(
                enrollsByBoardId.values().stream().flatMap(List::stream).toList());
        Map<Long, ClubInfo> clubById = findClubs(applicantById.values());
        List<EnrollReceivedResult> content = boardIds.stream()
                .filter(boardId -> enrollsByBoardId.containsKey(boardId) && boardById.containsKey(boardId))
                .map(boardId -> new EnrollReceivedResult(
                        boardById.get(boardId),
                        enrollsByBoardId.get(boardId).stream()
                                .map(enroll -> {
                                    UserInfo applicant = applicantById.get(enroll.getUserId());
                                    return EnrollView.of(enroll, applicant, clubOf(applicant, clubById));
                                })
                                .toList()))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public EnrollPendingCountResult getPendingEnrollCount(Long userId) {
        return new EnrollPendingCountResult(enrollRepository.countPendingByOwnerId(userId));
    }

    private Map<Long, UserInfo> findApplicants(List<Enroll> enrolls) {
        List<Long> userIds = enrolls.stream().map(Enroll::getUserId).distinct().toList();
        return userIds.isEmpty() ? Map.of() : userQueryApi.getInfos(userIds);
    }

    private Map<Long, ClubInfo> findClubs(Collection<UserInfo> users) {
        List<Long> clubIds = users.stream()
                .map(UserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return clubIds.isEmpty() ? Map.of() : clubQueryApi.getInfos(clubIds);
    }

    private static ClubInfo clubOf(UserInfo user, Map<Long, ClubInfo> clubById) {
        return user != null && user.clubId() != null ? clubById.get(user.clubId()) : null;
    }

    // 게시글 요약을 조립한다. 작성자·경기·구단을 BC 마다 한 번씩만 조회한다.
    private Map<Long, EnrollBoardResult> toBoardResults(Collection<BoardInfo> boards, Set<Long> bookmarkedIds) {
        if (boards.isEmpty()) {
            return Map.of();
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
        return boards.stream().collect(Collectors.toMap(BoardInfo::boardId, board -> {
            UserInfo writer = userById.get(board.userId());
            GameInfo game = board.gameId() != null ? gameById.get(board.gameId()) : null;
            return EnrollBoardResult.of(
                    board,
                    bookmarkedIds.contains(board.boardId()),
                    WriterView.of(writer, clubOf(writer, clubById)),
                    ClubView.from(board.cheerClubId() != null ? clubById.get(board.cheerClubId()) : null),
                    game == null
                            ? null
                            : GameView.of(
                                    game,
                                    game.homeClubId() != null ? clubById.get(game.homeClubId()) : null,
                                    game.awayClubId() != null ? clubById.get(game.awayClubId()) : null));
        }));
    }
}
