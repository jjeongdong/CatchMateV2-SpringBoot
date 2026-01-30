package com.back.catchmate.application.board;

import com.back.catchmate.application.board.dto.command.BoardCreateCommand;
import com.back.catchmate.application.board.dto.command.BoardUpdateCommand;
import com.back.catchmate.application.board.dto.command.GameCreateCommand;
import com.back.catchmate.application.board.dto.command.GameUpdateCommand;
import com.back.catchmate.application.board.dto.response.BoardCreateResponse;
import com.back.catchmate.application.board.dto.response.BoardDetailResponse;
import com.back.catchmate.application.board.dto.response.BoardLiftUpResponse;
import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.board.dto.response.BoardTempDetailResponse;
import com.back.catchmate.application.board.dto.response.BoardUpdateResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.bookmark.service.BookmarkService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.enroll.service.EnrollService;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.service.GameService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.BlockService;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardUseCase {
    private final BoardService boardService;
    private final UserService userService;
    private final ClubService clubService;
    private final GameService gameService;
    private final BlockService blockService;
    private final EnrollService enrollService;
    private final BookmarkService bookmarkService;

    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        // 기존 임시저장 글이 있다면 삭제 처리
        Optional<Board> oldDraft = boardService.findTempBoard(userId);
        oldDraft.ifPresent(boardService::deleteBoard);

        User user = userService.getUserById(userId);

        Club cheerClub = getCheerClub(command.getCheerClubId());
        Game game = getGame(command.getGameCreateCommand());

        // 도메인 객체 생성
        Board board = Board.createBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                user,
                cheerClub,
                game,
                command.getPreferredGender(),
                command.getPreferredAgeRange(),
                command.isCompleted()
        );

        // 게시글 저장
        Board savedBoard = boardService.createBoard(board);
        return BoardCreateResponse.of(savedBoard.getId());
    }

    public BoardDetailResponse getBoardDetail(Long userId, Long boardId) {
        User user = userService.getUserById(userId);
        Board board = boardService.getBoard(boardId);

        // 찜 여부 확인
        boolean isBookMarked = bookmarkService.isBookmarked(userId, boardId);

        // 버튼 상태 계산
        String buttonStatus = getButtonStatus(user, board);

        // TODO: ChatService 연동 채팅방 ID 조회
        Long chatRoomId = null;

        return BoardDetailResponse.of(board, isBookMarked, buttonStatus, chatRoomId);
    }

    public PagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson, List<Long> preferredTeamIdList, int page, int size) {
        // 차단된 유저 ID 목록 조회
        User user = userService.getUserById(userId);
        List<Long> blockedUserIds = blockService.getBlockedUserIds(user);

        // 검색 조건 생성
        BoardSearchCondition condition = BoardSearchCondition.of(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList != null ? preferredTeamIdList : Collections.emptyList(),
                blockedUserIds
        );

        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(condition, domainPageable);

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> {
                    boolean isBookMarked = bookmarkService.isBookmarked(userId, board.getId());
                    return BoardResponse.of(board, isBookMarked);
                })
                .toList();

        return new PagedResponse<>(boardPage, boardResponses);
    }

    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        User targetUser = userService.getUserById(targetUserId);
        User loginUser = userService.getUserById(loginUserId);

        // 차단 여부 확인
        if (blockService.isUserBlocked(targetUser, loginUser)) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);

        // 서비스 호출
        DomainPage<Board> boardPage = boardService.getBoardListByUserId(targetUserId, domainPageable);

        // 응답 DTO 변환
        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> {
                    boolean isBookMarked = false;
                    return BoardResponse.of(board, isBookMarked);
                })
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public BoardTempDetailResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = boardService.findTempBoard(userId);
        return tempBoard.map(BoardTempDetailResponse::from).orElse(null);
    }

    @Transactional
    public BoardUpdateResponse updateBoard(Long userId, Long boardId, BoardUpdateCommand command) {
        Board board = boardService.getBoard(boardId);

        Club cheerClub = getCheerClub(command.getCheerClubId());
        Game game = getGame(command.getGameUpdateCommand());

        // 도메인 로직 실행
        board.updateBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                cheerClub,
                game,
                command.getPreferredGender(),
                command.getPreferredAgeRange(),
                command.isCompleted()
        );

        // 변경사항 저장
        boardService.updateBoard(board);
        return BoardUpdateResponse.of(board.getId());
    }

    @Transactional
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextLiftUpAllowed = board.getLiftUpDate().plusDays(3);

        if (nextLiftUpAllowed.isBefore(now)) {
            board.updateLiftUpDate(now);
            boardService.updateBoard(board);
            return BoardLiftUpResponse.of(true, null);
        }

        long remainingMinutes = Duration.between(now, nextLiftUpAllowed).toMinutes();
        return BoardLiftUpResponse.of(false, formatRemainingTime(remainingMinutes));
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        // 게시글 조회
        Board board = boardService.getBoard(boardId);

        // 도메인 로직 실행
        board.delete();

        // 변경 사항 저장
        boardService.updateBoard(board);
    }

    // =================================================================================
    // Private Helpers
    // =================================================================================

    private Club getCheerClub(Long clubId) {
        if (clubId == null) return null;
        return clubService.getClub(clubId);
    }

    private Game getGame(GameCreateCommand command) {
        if (command == null) return null;
        return fetchGame(
                command.getHomeClubId(),
                command.getAwayClubId(),
                command.getGameStartDate(),
                command.getLocation()
        );
    }

    private Game getGame(GameUpdateCommand command) {
        if (command == null) return null;
        return fetchGame(
                command.getHomeClubId(),
                command.getAwayClubId(),
                command.getGameStartDate(),
                command.getLocation()
        );
    }

    // 공통 Game 조회/생성 로직 추출 (중복 제거)
    private Game fetchGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        Club homeClub = clubService.getClub(homeClubId);
        Club awayClub = clubService.getClub(awayClubId);

        return gameService.findOrCreateGame(
                homeClub,
                awayClub,
                gameStartDate,
                location
        );
    }

    private String getButtonStatus(User user, Board board) {
        // 작성자 본인인 경우
        if (board.getUser().getId().equals(user.getId())) {
            return "VIEW_CHAT";
        }

        Optional<Enroll> enrollOptional = enrollService.getEnrollByUserAndBoard(user, board);

        // 신청 내역이 없는 경우 -> 신청 가능
        if (enrollOptional.isEmpty()) {
            return "APPLY";
        }

        Enroll enroll = enrollOptional.get();
        return switch (enroll.getAcceptStatus()) {
            case ACCEPTED -> "VIEW_CHAT"; // 수락됨 -> 채팅방 보기
            case PENDING -> "CANCEL";    // 대기중 -> 신청 취소
            case REJECTED -> "REJECTED";  // 거절됨 -> 거절됨
            default -> "APPLY";
        };
    }

    private String formatRemainingTime(long remainingMinutes) {
        long days = remainingMinutes / 1440;
        long hours = (remainingMinutes % 1440) / 60;
        long minutes = remainingMinutes % 60;

        return String.format("%d일 %02d시간 %02d분", days, hours, minutes);
    }
}
