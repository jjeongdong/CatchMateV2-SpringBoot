package com.back.catchmate.enroll.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.enroll.application.dto.response.ApplicantResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollApplicantDetailView;
import com.back.catchmate.enroll.application.dto.response.EnrollApplicantResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollBoardSummary;
import com.back.catchmate.enroll.application.dto.response.EnrollClubView;
import com.back.catchmate.enroll.application.dto.response.EnrollCountResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollDetailResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollGameView;
import com.back.catchmate.enroll.application.dto.response.EnrollReceiveResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollRequestResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollResponse;
import com.back.catchmate.enroll.application.dto.response.EnrollWriterView;
import com.back.catchmate.enroll.application.port.in.EnrollClientQueryUseCase;
import com.back.catchmate.enroll.application.port.out.external.BoardFetchPort;
import com.back.catchmate.enroll.application.port.out.external.BookmarkFetchPort;
import com.back.catchmate.enroll.application.port.out.external.ClubFetchPort;
import com.back.catchmate.enroll.application.port.out.external.GameFetchPort;
import com.back.catchmate.enroll.application.port.out.external.UserFetchPort;
import com.back.catchmate.enroll.application.port.out.dto.EnrollBoardInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollClubInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollGameInfo;
import com.back.catchmate.enroll.application.port.out.dto.EnrollUserInfo;
import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
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
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class EnrollClientQueryService implements EnrollClientQueryUseCase {
    private final EnrollReader enrollReader;
    private final BoardFetchPort boardFetchPort;
    private final BookmarkFetchPort bookmarkFetchPort;
    private final ClubFetchPort clubFetchPort;
    private final GameFetchPort gameFetchPort;
    private final UserFetchPort userFetchPort;

    @Override
    public EnrollDetailResponse getEnroll(Long userId, Long enrollId) {
        Enroll enroll = enrollReader.getEnroll(enrollId);
        Long applicantId = enroll.getUserId();
        EnrollBoardInfo board = boardFetchPort.getBoard(enroll.getBoardId());
        Long writerId = board.userId();

        if (!userId.equals(applicantId) && !userId.equals(writerId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        EnrollUserInfo applicant = userFetchPort.getUser(applicantId);
        EnrollClubInfo applicantClub = applicant.clubId() != null ? clubFetchPort.getClub(applicant.clubId()) : null;
        EnrollBoardSummary boardSummary = buildBoardSummary(board, false);
        return toEnrollDetailResponse(enroll, applicant, applicantClub, boardSummary);
    }

    @Override
    public PagedResponse<EnrollRequestResponse> getEnrollRequestList(Long userId, int page, int size) {
        Page<Enroll> enrollPage = enrollReader.getEnrollListByUserId(userId, PageRequest.of(page, size));

        List<Long> boardIds = enrollPage.getContent().stream()
                .map(Enroll::getBoardId)
                .distinct()
                .toList();

        Set<Long> bookmarkedBoardIds = boardIds.isEmpty() ? Set.of() : bookmarkFetchPort.findBookmarkedBoardIds(userId, boardIds);
        List<EnrollBoardInfo> boards = boardIds.isEmpty() ? List.of() : boardFetchPort.getBoards(boardIds);
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

    @Override
    public PagedResponse<EnrollApplicantResponse> getEnrollReceiveListByBoardId(Long userId, Long boardId, int page, int size) {
        EnrollBoardInfo board = boardFetchPort.getBoard(boardId);
        if (!board.userId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Page<Enroll> enrollPage = enrollReader.getEnrollListByBoardIdAndStatus(boardId, AcceptStatus.PENDING, PageRequest.of(page, size));

        Map<Long, EnrollUserInfo> userById = resolveEnrollApplicants(enrollPage.getContent());
        Map<Long, EnrollClubInfo> clubById = resolveClubs(userById.values());
        List<EnrollApplicantResponse> responses = enrollPage.getContent().stream()
                .map(enroll -> {
                    EnrollUserInfo u = userById.get(enroll.getUserId());
                    EnrollClubInfo c = u != null && u.clubId() != null ? clubById.get(u.clubId()) : null;
                    return toEnrollApplicantResponse(enroll, u, c);
                })
                .toList();

        return new PagedResponse<>(enrollPage, responses);
    }

    @Override
    public PagedResponse<EnrollReceiveResponse> getEnrollReceiveList(Long userId, int page, int size) {
        Page<Long> boardIdPage = enrollReader.getBoardIdsWithPendingEnrolls(userId, PageRequest.of(page, size));
        List<Long> boardIds = boardIdPage.getContent();

        if (boardIds.isEmpty()) {
            return new PagedResponse<>(boardIdPage, Collections.emptyList());
        }

        List<Enroll> allEnrolls = enrollReader.getEnrollListByBoardIds(boardIds);

        List<EnrollReceiveResponse> content = boardIds.stream()
                .map(boardId -> mapToEnrollReceiveResponse(boardId, allEnrolls))
                .filter(Objects::nonNull)
                .toList();

        return new PagedResponse<>(boardIdPage, content);
    }

    @Override
    public EnrollCountResponse getEnrollPendingCount(Long userId) {
        long count = enrollReader.getEnrollPendingCountByBoardWriter(userId);
        return EnrollCountResponse.of(count);
    }

    // --- Internal Helpers ---

    private EnrollDetailResponse toEnrollDetailResponse(Enroll enroll, EnrollUserInfo applicant, EnrollClubInfo applicantClub, EnrollBoardSummary boardResponse) {
        return new EnrollDetailResponse(
                enroll.getId(),
                enroll.getAcceptStatus(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                toApplicantDetailView(applicant, applicantClub),
                boardResponse
        );
    }

    private EnrollApplicantResponse toEnrollApplicantResponse(Enroll enroll, EnrollUserInfo user, EnrollClubInfo club) {
        return new EnrollApplicantResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.getRequestedAt(),
                true,
                ApplicantResponse.from(user, club)
        );
    }

    private EnrollResponse toEnrollResponse(Enroll enroll, EnrollUserInfo user, EnrollClubInfo club) {
        return new EnrollResponse(
                enroll.getId(),
                enroll.getDescription(),
                enroll.isNewEnroll(),
                enroll.getRequestedAt(),
                ApplicantResponse.from(user, club)
        );
    }

    private EnrollReceiveResponse mapToEnrollReceiveResponse(Long boardId, List<Enroll> allEnrolls) {
        List<Enroll> enrolls = allEnrolls.stream()
                .filter(e -> e.getBoardId().equals(boardId))
                .toList();

        if (enrolls.isEmpty()) return null;

        EnrollBoardInfo board = boardFetchPort.getBoard(boardId);
        EnrollBoardSummary boardSummary = buildBoardSummary(board, false);

        Map<Long, EnrollUserInfo> userById = resolveEnrollApplicants(enrolls);
        Map<Long, EnrollClubInfo> clubById = resolveClubs(userById.values());
        List<EnrollResponse> enrollList = enrolls.stream()
                .map(e -> {
                    EnrollUserInfo u = userById.get(e.getUserId());
                    EnrollClubInfo c = u != null && u.clubId() != null ? clubById.get(u.clubId()) : null;
                    return toEnrollResponse(e, u, c);
                })
                .toList();

        return EnrollReceiveResponse.of(boardSummary, enrollList);
    }

    private Map<Long, EnrollUserInfo> resolveEnrollApplicants(List<Enroll> enrolls) {
        List<Long> userIds = enrolls.stream()
                .map(Enroll::getUserId)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return userFetchPort.getUsers(userIds).stream()
                .collect(Collectors.toMap(EnrollUserInfo::userId, Function.identity()));
    }

    private Map<Long, EnrollClubInfo> resolveClubs(Collection<EnrollUserInfo> users) {
        List<Long> clubIds = users.stream()
                .map(EnrollUserInfo::clubId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (clubIds.isEmpty()) return Map.of();
        return clubFetchPort.getClubs(clubIds).stream()
                .collect(Collectors.toMap(EnrollClubInfo::clubId, Function.identity()));
    }

    private EnrollBoardSummary buildBoardSummary(EnrollBoardInfo board, boolean bookmarked) {
        return buildBoardSummaries(List.of(board), id -> bookmarked).get(0);
    }

    private List<EnrollBoardSummary> buildBoardSummaries(List<EnrollBoardInfo> boards, Predicate<Long> bookmarkedPredicate) {
        if (boards.isEmpty()) return List.of();

        List<Long> userIds = boards.stream()
                .map(EnrollBoardInfo::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Long> gameIds = boards.stream()
                .map(EnrollBoardInfo::gameId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, EnrollUserInfo> userMap = userIds.isEmpty() ? Map.of() :
                userFetchPort.getUsers(userIds).stream()
                        .collect(Collectors.toMap(EnrollUserInfo::userId, Function.identity()));
        Map<Long, EnrollGameInfo> gameMap = gameIds.isEmpty() ? Map.of() :
                gameFetchPort.getGames(gameIds).stream()
                        .collect(Collectors.toMap(EnrollGameInfo::gameId, Function.identity()));

        List<Long> clubIds = Stream.of(
                        boards.stream().map(EnrollBoardInfo::cheerClubId),
                        gameMap.values().stream().map(EnrollGameInfo::homeClubId),
                        gameMap.values().stream().map(EnrollGameInfo::awayClubId),
                        userMap.values().stream().map(EnrollUserInfo::clubId)
                )
                .flatMap(Function.identity())
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, EnrollClubInfo> clubMap = clubIds.isEmpty() ? Map.of() :
                clubFetchPort.getClubs(clubIds).stream()
                        .collect(Collectors.toMap(EnrollClubInfo::clubId, Function.identity()));

        return boards.stream()
                .map(board -> toSummary(board, bookmarkedPredicate.test(board.boardId()), userMap, clubMap, gameMap))
                .toList();
    }

    private EnrollBoardSummary toSummary(EnrollBoardInfo board, boolean bookmarked,
                                         Map<Long, EnrollUserInfo> userMap,
                                         Map<Long, EnrollClubInfo> clubMap,
                                         Map<Long, EnrollGameInfo> gameMap) {
        EnrollUserInfo user = board.userId() != null ? userMap.get(board.userId()) : null;
        EnrollClubInfo userClub = user != null && user.clubId() != null ? clubMap.get(user.clubId()) : null;
        EnrollClubInfo cheerClub = board.cheerClubId() != null ? clubMap.get(board.cheerClubId()) : null;
        EnrollGameInfo game = board.gameId() != null ? gameMap.get(board.gameId()) : null;
        EnrollClubInfo homeClub = game != null && game.homeClubId() != null ? clubMap.get(game.homeClubId()) : null;
        EnrollClubInfo awayClub = game != null && game.awayClubId() != null ? clubMap.get(game.awayClubId()) : null;
        return toEnrollBoardSummary(board, bookmarked, user, userClub, cheerClub, game, homeClub, awayClub);
    }

    private EnrollBoardSummary toEnrollBoardSummary(EnrollBoardInfo board, boolean bookMarked, EnrollUserInfo user, EnrollClubInfo userClub,
                                                    EnrollClubInfo cheerClub, EnrollGameInfo game, EnrollClubInfo homeClub, EnrollClubInfo awayClub) {
        return new EnrollBoardSummary(
                board.boardId(),
                board.title(),
                board.content(),
                board.currentPerson(),
                board.maxPerson(),
                bookMarked,
                toClubView(cheerClub),
                toGameView(game, homeClub, awayClub),
                toWriterView(user, userClub)
        );
    }

    private EnrollClubView toClubView(EnrollClubInfo club) {
        if (club == null) return null;
        return new EnrollClubView(club.clubId(), club.name(), club.homeStadium(), club.region());
    }

    private EnrollGameView toGameView(EnrollGameInfo game, EnrollClubInfo homeClub, EnrollClubInfo awayClub) {
        if (game == null) return null;
        return new EnrollGameView(
                game.gameId(),
                game.gameStartDate(),
                game.location(),
                toClubView(homeClub),
                toClubView(awayClub)
        );
    }

    private EnrollWriterView toWriterView(EnrollUserInfo user, EnrollClubInfo userClub) {
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

    private EnrollApplicantDetailView toApplicantDetailView(EnrollUserInfo user, EnrollClubInfo userClub) {
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
}
