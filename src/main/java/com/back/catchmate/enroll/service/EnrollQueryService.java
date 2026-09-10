package com.back.catchmate.enroll.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.board.dto.response.BoardSummary;
import com.back.catchmate.board.service.BoardService;
import com.back.catchmate.bookmark.service.BookmarkService;
import com.back.catchmate.club.dto.response.ClubSummary;
import com.back.catchmate.club.service.ClubService;
import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.entity.AcceptStatus;
import com.back.catchmate.enroll.entity.Enroll;
import com.back.catchmate.enroll.repository.EnrollRepository;
import com.back.catchmate.game.dto.response.GameSummary;
import com.back.catchmate.game.service.GameService;
import com.back.catchmate.user.dto.response.UserSummary;
import com.back.catchmate.user.service.UserService;
import org.springframework.data.domain.Sort;
import java.util.HashSet;
import com.back.catchmate.enroll.dto.response.ApplicantResponse;
import com.back.catchmate.enroll.dto.response.EnrollApplicantDetailView;
import com.back.catchmate.enroll.dto.response.EnrollApplicantResponse;
import com.back.catchmate.enroll.dto.response.EnrollBoardSummary;
import com.back.catchmate.enroll.dto.response.EnrollClubView;
import com.back.catchmate.enroll.dto.response.EnrollCountResponse;
import com.back.catchmate.enroll.dto.response.EnrollDetailResponse;
import com.back.catchmate.enroll.dto.response.EnrollGameView;
import com.back.catchmate.enroll.dto.response.EnrollReceiveResponse;
import com.back.catchmate.enroll.dto.response.EnrollRequestResponse;
import com.back.catchmate.enroll.dto.response.EnrollResponse;
import com.back.catchmate.enroll.dto.response.EnrollSummary;
import com.back.catchmate.enroll.dto.response.EnrollWriterView;
import com.google.common.math.LongMath;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollQueryService {
    private final EnrollRepository enrollRepository;

    private final BookmarkService bookmarkService;
    private final ClubService clubService;
    private final GameService gameService;
    private final UserService userService;
    private final BoardService boardService;

    public EnrollDetailResponse getEnroll(Long userId, Long enrollId) {
        Enroll enroll = getEnrollOrThrow(enrollId);
        Long applicantId = enroll.getUserId();
        BoardSummary board = boardService.getBoardSummary(enroll.getBoardId());
        Long writerId = board.userId();

        if (!userId.equals(applicantId) && !userId.equals(writerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        UserSummary applicant = userService.getUserSummary(applicantId);
        ClubSummary applicantClub = applicant.clubId() != null ? clubService.getClubSummary(applicant.clubId()) : null;
        EnrollBoardSummary boardSummary = buildBoardSummary(board, false);
        return toEnrollDetailResponse(enroll, applicant, applicantClub, boardSummary);
    }

    public PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size) {
        Page<Enroll> enrollPage = enrollRepository.findAllByUserId(userId, latestFirst(page, size));

        List<Long> boardIds = enrollPage.getContent().stream()
                .map(Enroll::getBoardId)
                .distinct()
                .toList();

        Set<Long> bookmarkedBoardIds = boardIds.isEmpty() ? Set.of() : new HashSet<>(bookmarkService.findBookmarkedBoardIds(userId, boardIds));
        List<BoardSummary> boards = boardIds.isEmpty() ? List.of() : boardService.getBoardSummaries(boardIds);
        Map<Long, EnrollBoardSummary> boardSummaryById = buildBoardSummaries(boards, bookmarkedBoardIds::contains)
                .stream()
                .collect(Collectors.toMap(EnrollBoardSummary::boardId, Function.identity()));

        List<EnrollRequestResponse> responses = enrollPage.getContent().stream()
                .map(enroll -> EnrollRequestResponse.from(
                        enroll,
                        boardSummaryById.get(enroll.getBoardId())
                ))
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollApplicantResponse> getEnrollReceiveListByBoardId(Long userId, Long boardId, int page, int size) {
        BoardSummary board = boardService.getBoardSummary(boardId);
        if (!board.userId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Page<Enroll> enrollPage = enrollRepository.findAllByBoardIdAndAcceptStatus(boardId, AcceptStatus.PENDING, latestFirst(page, size));

        Map<Long, UserSummary> userById = resolveEnrollApplicants(enrollPage.getContent());
        Map<Long, ClubSummary> clubById = resolveClubs(userById.values());
        List<EnrollApplicantResponse> responses = enrollPage.getContent().stream()
                .map(enroll -> {
                    UserSummary u = userById.get(enroll.getUserId());
                    ClubSummary c = u != null && u.clubId() != null ? clubById.get(u.clubId()) : null;
                    return toEnrollApplicantResponse(enroll, u, c);
                })
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    public PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size) {
        // 사용자가 작성한 게시글 중에서 신청이 보류 중인 게시글의 ID를 가져온다.
        Page<Long> boardIdPage = enrollRepository.findDistinctBoardIdsByOwnerIdAndStatus(userId, AcceptStatus.PENDING, PageRequest.of(page, size));
        List<Long> boardIds = boardIdPage.getContent();

        if (boardIds.isEmpty()) {
            return new PagedResponse<>(boardIdPage, Collections.emptyList());
        }

        List<Enroll> allEnrolls = findPendingEnrollsByBoardIds(boardIds);
        Map<Long, List<Enroll>> enrollsByBoardId = allEnrolls.stream()
                .collect(Collectors.groupingBy(Enroll::getBoardId));

        // 페이지 전체를 한 번에 조립 — 보드 1회 + (작성자·경기·구단) 각 1회
        Map<Long, EnrollBoardSummary> summaryByBoardId =
                buildBoardSummaries(boardService.getBoardSummaries(boardIds), id -> false).stream()
                        .collect(Collectors.toMap(EnrollBoardSummary::boardId, Function.identity()));

        // 신청자·신청자 구단도 페이지 전체를 한 번에
        Map<Long, UserSummary> applicantById = resolveEnrollApplicants(allEnrolls);
        Map<Long, ClubSummary> applicantClubById = resolveClubs(applicantById.values());

        List<EnrollReceiveResponse> content = boardIds.stream()
                .map(boardId -> {
                    List<Enroll> enrolls = enrollsByBoardId.get(boardId);
                    EnrollBoardSummary boardSummary = summaryByBoardId.get(boardId);
                    if (enrolls == null || boardSummary == null) return null;

                    List<EnrollResponse> enrollList = enrolls.stream()
                            .map(e -> {
                                UserSummary u = applicantById.get(e.getUserId());
                                ClubSummary c = u != null && u.clubId() != null ? applicantClubById.get(u.clubId()) : null;
                                return toEnrollResponse(e, u, c);
                            })
                            .toList();

                    return EnrollReceiveResponse.of(boardSummary, enrollList);
                })
                .filter(Objects::nonNull)
                .toList();

        return new PagedResponse<>(boardIdPage, content);
    }

    public EnrollCountResponse getEnrollPendingCount(Long userId) {
        long count = enrollRepository.countByBoardOwnerIdAndAcceptStatus(userId, AcceptStatus.PENDING);
        return EnrollCountResponse.of(count);
    }

    // --- Internal Helpers ---

    private EnrollDetailResponse toEnrollDetailResponse(Enroll enroll, UserSummary applicant, ClubSummary applicantClub, EnrollBoardSummary boardResponse) {
        return new EnrollDetailResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                toApplicantDetailView(applicant, applicantClub),
                boardResponse
        );
    }

    private EnrollApplicantResponse toEnrollApplicantResponse(Enroll enroll, UserSummary user, ClubSummary club) {
        return new EnrollApplicantResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                true,
                ApplicantResponse.from(user, club)
        );
    }

    private EnrollResponse toEnrollResponse(Enroll enroll, UserSummary user, ClubSummary club) {
        return new EnrollResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.isNewEnroll(),
                enroll.getRequestedAt(),
                ApplicantResponse.from(user, club)
        );
    }

    private Map<Long, UserSummary> resolveEnrollApplicants(List<Enroll> enrolls) {
        List<Long> userIds = enrolls.stream()
                .map(Enroll::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return userService.getUserSummaries(userIds).stream()
                .collect(Collectors.toMap(UserSummary::userId, Function.identity()));
    }

    private Map<Long, ClubSummary> resolveClubs(Collection<UserSummary> users) {
        List<Long> clubIds = users.stream()
                .map(UserSummary::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) return Map.of();
        return clubService.getClubSummaries(clubIds).stream()
                .collect(Collectors.toMap(ClubSummary::clubId, Function.identity()));
    }

    private EnrollBoardSummary buildBoardSummary(BoardSummary board, boolean bookmarked) {
        return buildBoardSummaries(List.of(board), id -> bookmarked).get(0);
    }

    private List<EnrollBoardSummary> buildBoardSummaries(List<BoardSummary> boards, Predicate<Long> bookmarkedPredicate) {
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

        Map<Long, UserSummary> userMap = userIds.isEmpty() ? Map.of() :
                userService.getUserSummaries(userIds).stream()
                        .collect(Collectors.toMap(UserSummary::userId, Function.identity()));
        Map<Long, GameSummary> gameMap = gameIds.isEmpty() ? Map.of() :
                gameService.getGameSummaries(gameIds).stream()
                        .collect(Collectors.toMap(GameSummary::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(BoardSummary::cheerClubId),
                        gameMap.values().stream().map(GameSummary::homeClubId),
                        gameMap.values().stream().map(GameSummary::awayClubId),
                        userMap.values().stream().map(UserSummary::clubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ClubSummary> clubMap = clubIds.isEmpty() ? Map.of() :
                clubService.getClubSummaries(clubIds).stream()
                        .collect(Collectors.toMap(ClubSummary::clubId, Function.identity()));

        return boards.stream()
                .map(board -> toSummary(board, bookmarkedPredicate.test(board.boardId()), userMap, clubMap, gameMap))
                .toList();
    }

    private EnrollBoardSummary toSummary(BoardSummary board, boolean bookmarked,
                                         Map<Long, UserSummary> userMap,
                                         Map<Long, ClubSummary> clubMap,
                                         Map<Long, GameSummary> gameMap) {
        UserSummary user = board.userId() != null ? userMap.get(board.userId()) : null;
        ClubSummary userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        ClubSummary cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        GameSummary game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        ClubSummary homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        ClubSummary awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return toEnrollBoardSummary(board, bookmarked, user, userClub, cheerClub, game, homeClub, awayClub);
    }

    private EnrollBoardSummary toEnrollBoardSummary(BoardSummary board, boolean bookMarked, UserSummary user, ClubSummary userClub,
                                                    ClubSummary cheerClub, GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        return new EnrollBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                // maxPerson 은 board 쪽이 Integer 라 널이 올 수 있다. 언박싱 NPE 를 막는 기본값 0.
                board.maxPerson() != null ? board.maxPerson() : 0,
                bookMarked,
                toClubView(cheerClub),
                toGameView(game, homeClub, awayClub),
                toWriterView(user, userClub)
        );
    }

    private EnrollClubView toClubView(ClubSummary club) {
        if (club == null) return null;
        return new EnrollClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private EnrollGameView toGameView(GameSummary game, ClubSummary homeClub, ClubSummary awayClub) {
        if (game == null) return null;
        return new EnrollGameView(
                game.gameId(),
                game.gameStartDate(),
                game.location(),
                toClubView(homeClub),
                toClubView(awayClub)
        );
    }

    private EnrollWriterView toWriterView(UserSummary user, ClubSummary userClub) {
        if (user == null) return null;
        return new EnrollWriterView(
                user.userId(),
                user.nickName(),
                user.email(),
                user.profileImageUrl(),
                user.gender() != null ? user.gender() : ' ',
                user.birthDate(),
                user.watchStyle(),
                toClubView(userClub),
                user.authority()
        );
    }

    private EnrollApplicantDetailView toApplicantDetailView(UserSummary user, ClubSummary userClub) {
        if (user == null) return null;
        return new EnrollApplicantDetailView(
                user.userId(),
                user.nickName(),
                user.email(),
                user.profileImageUrl(),
                user.gender() != null ? user.gender() : ' ',
                user.birthDate(),
                user.watchStyle(),
                toClubView(userClub),
                user.authority()
        );
    }

    // ── 다른 컨텍스트용 ────────────────────────────────────────────────
    public EnrollSummary getEnrollSummary(Long enrollId) {
        return toSummary(getEnrollOrThrow(enrollId));
    }

    public Optional<EnrollSummary> findEnrollByUserIdAndBoardId(Long userId, Long boardId) {
        return enrollRepository.findByUserIdAndBoardId(userId, boardId).map(this::toSummary);
    }

    public Optional<String> findAcceptStatusById(Long enrollId) {
        return enrollRepository.findAcceptStatusById(enrollId).map(AcceptStatus::name);
    }

    public Map<Long, String> getAcceptStatusMapByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        return enrollRepository.findIdAndAcceptStatusByIdIn(ids).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((AcceptStatus) row[1]).name()));
    }

    public List<EnrollSummary> getEnrollListByBoardIds(List<Long> boardIds) {
        return findPendingEnrollsByBoardIds(boardIds).stream()
                .map(this::toSummary)
                .toList();
    }

    // ── 내부 헬퍼 ─────────────────────────────────────────────────────
    private EnrollSummary toSummary(Enroll enroll) {
        return new EnrollSummary(
                enroll.getId(),
                enroll.getUserId(),
                enroll.getBoardId(),
                enroll.getDescription(),
                enroll.getAcceptStatus() != null ? enroll.getAcceptStatus().name() : null,
                enroll.isNewEnroll(),
                enroll.getRequestedAt()
        );
    }

    private Enroll getEnrollOrThrow(Long enrollId) {
        return enrollRepository.findById(enrollId)
                .orElseThrow(() -> new BaseException(ErrorCode.ENROLL_NOT_FOUND));
    }

    private List<Enroll> findPendingEnrollsByBoardIds(List<Long> boardIds) {
        if (boardIds == null || boardIds.isEmpty()) return List.of();
        return enrollRepository.findAllByBoardIdInAndStatus(boardIds, AcceptStatus.PENDING);
    }

    // 목록 조회는 호출자가 넘긴 정렬을 무시하고 항상 최신순으로 고정한다.
    // (기존 EnrollRepositoryImpl.findAllByUserId / findAllByBoardIdAndStatus 동작을 그대로 옮긴 것)
    private PageRequest latestFirst(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
