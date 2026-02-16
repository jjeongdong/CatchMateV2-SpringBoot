package com.back.catchmate.orchestration.board;

import com.back.catchmate.application.board.service.BoardService;
import com.back.catchmate.application.bookmark.service.BookmarkService;
import com.back.catchmate.application.chat.service.ChatRoomMemberService;
import com.back.catchmate.application.chat.service.ChatRoomService;
import com.back.catchmate.application.club.service.ClubService;
import com.back.catchmate.application.enroll.service.EnrollService;
import com.back.catchmate.application.user.service.BlockService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.model.BoardButtonStatus;
import com.back.catchmate.domain.chat.model.ChatRoom;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.service.GameService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.board.dto.command.BoardCreateCommand;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import com.back.catchmate.orchestration.board.dto.command.BoardUpdateCommand;
import com.back.catchmate.orchestration.board.dto.command.GameCreateCommand;
import com.back.catchmate.orchestration.board.dto.command.GameUpdateCommand;
import com.back.catchmate.orchestration.board.dto.response.BoardCreateResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardDetailResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardLiftUpResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardTempDetailResponse;
import com.back.catchmate.orchestration.board.dto.response.BoardUpdateResponse;
import com.back.catchmate.orchestration.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardOrchestrator {
    private final ClubService clubService;
    private final GameService gameService;
    private final UserService userService;
    private final BlockService blockService;
    private final BoardService boardService;
    private final EnrollService enrollService;
    private final BookmarkService bookmarkService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;

    @Transactional
    public BoardCreateResponse createBoard(Long userId, BoardCreateCommand command) {
        // 기존 임시저장 글 삭제
        Optional<Board> oldDraft = boardService.findTempBoard(userId);
        oldDraft.ifPresent(boardService::deleteBoard);

        User user = userService.getUser(userId);
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

        Board savedBoard = boardService.createBoard(board);

        // 채팅방 생성 및 멤버 추가
        if (command.isCompleted()) {
            ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(savedBoard);
            chatRoomMemberService.addMember(chatRoom, user);
        }

        return BoardCreateResponse.of(savedBoard.getId());
    }

    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        User user = userService.getUser(userId);
        Board board = boardService.getBoard(boardId);
        boolean isBookMarked = bookmarkService.isBookmarked(userId, boardId);

        Optional<Enroll> myEnroll = enrollService.findEnrollByUserAndBoard(user, board);
        BoardButtonStatus buttonStatus = getButtonStatus(user, board, myEnroll);
        Long myEnrollId = myEnroll.map(Enroll::getId).orElse(null);

        Long chatRoomId = board.isCompleted()
                ? chatRoomService.getOrCreateChatRoom(board).getId()
                : null;

        return BoardDetailResponse.from(board, isBookMarked, buttonStatus, myEnrollId, chatRoomId);
    }

    public PagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson, List<Long> preferredTeamIdList, int page, int size) {
        User user = userService.getUser(userId);
        List<Long> blockedUserIds = blockService.getBlockedUserIds(user);

        BoardSearchCondition condition = BoardSearchCondition.of(
                userId,
                gameDate,
                maxPerson,
                preferredTeamIdList,
                blockedUserIds
        );

        DomainPageable domainPageable = DomainPageable.of(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(condition, domainPageable);

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> {
                    boolean isBookMarked = bookmarkService.isBookmarked(userId, board.getId());
                    return BoardResponse.from(board, isBookMarked);
                })
                .toList();

        return new PagedResponse<>(boardPage, boardResponses);
    }

    public PagedResponse<BoardResponse> getBoardListByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        User targetUser = userService.getUser(targetUserId);
        User loginUser = userService.getUser(loginUserId);

        if (blockService.isUserBlocked(targetUser, loginUser)) {
            throw new BaseException(ErrorCode.BLOCKED_USER_BOARD);
        }

        DomainPageable domainPageable = DomainPageable.of(page, size);
        DomainPage<Board> boardPage = boardService.getBoardListByUserId(targetUserId, domainPageable);

        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> {
                    boolean isBookMarked = bookmarkService.isBookmarked(loginUserId, board.getId());
                    return BoardResponse.from(board, isBookMarked);
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
        User user = userService.getUser(userId);

        boolean wasCompleted = board.isCompleted();
        Club cheerClub = getCheerClub(command.getCheerClubId());
        Game game = getGame(command.getGameUpdateCommand());

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

        boardService.updateBoard(board);

        if (!wasCompleted && command.isCompleted()) {
            ChatRoom chatRoom = chatRoomService.getOrCreateChatRoom(board);
            chatRoomMemberService.addMember(chatRoom, user);
        }

        return BoardUpdateResponse.of(board.getId());
    }

    @Transactional
    public BoardLiftUpResponse updateLiftUpDate(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);

        if (!board.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.BOARD_LIFT_UP_BAD_REQUEST);
        }

        if (board.canLiftUp()) {
            board.updateLiftUpDate(LocalDateTime.now());
            boardService.updateBoard(board);
            return BoardLiftUpResponse.of(true, null);
        }

        long remainingMinutes = board.getRemainingMinutesForLiftUp();
        return BoardLiftUpResponse.fromRemainingMinutes(false, remainingMinutes);
    }

    @Transactional
    public void deleteBoard(Long userId, Long boardId) {
        Board board = boardService.getBoard(boardId);
        if (!board.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.BOARD_DELETE_BAD_REQUEST);
        }
        board.delete();
        boardService.updateBoard(board);
    }

    // --- Private Helper Methods ---
    private Club getCheerClub(Long clubId) {
        if (clubId == null) return null;
        return clubService.getClub(clubId);
    }

    private Game getGame(GameCreateCommand command) {
        if (command == null) return null;
        if (command.getGameStartDate() != null || command.getHomeClubId() != null ||
                command.getAwayClubId() != null || command.getLocation() != null) {

            if (command.getHomeClubId() != null && command.getAwayClubId() != null && command.getGameStartDate() != null) {
                return fetchGame(command.getHomeClubId(), command.getAwayClubId(), command.getGameStartDate(), command.getLocation());
            } else {
                return createPartialGame(command.getGameStartDate(), command.getLocation(), command.getHomeClubId(), command.getAwayClubId());
            }
        }
        return null;
    }

    private Game getGame(GameUpdateCommand command) {
        if (command == null) return null;
        if (command.getGameStartDate() != null || command.getHomeClubId() != null ||
                command.getAwayClubId() != null || command.getLocation() != null) {

            if (command.getHomeClubId() != null && command.getAwayClubId() != null && command.getGameStartDate() != null) {
                return fetchGame(command.getHomeClubId(), command.getAwayClubId(), command.getGameStartDate(), command.getLocation());
            } else {
                return createPartialGame(command.getGameStartDate(), command.getLocation(), command.getHomeClubId(), command.getAwayClubId());
            }
        }
        return null;
    }

    private Game fetchGame(Long homeClubId, Long awayClubId, LocalDateTime gameStartDate, String location) {
        Club homeClub = clubService.getClub(homeClubId);
        Club awayClub = clubService.getClub(awayClubId);
        return gameService.findOrCreateGame(homeClub, awayClub, gameStartDate, location);
    }

    private Game createPartialGame(LocalDateTime gameStartDate, String location, Long homeClubId, Long awayClubId) {
        Club homeClub = homeClubId != null ? clubService.getClub(homeClubId) : null;
        Club awayClub = awayClubId != null ? clubService.getClub(awayClubId) : null;
        return gameService.savePartialGame(gameStartDate, location, homeClub, awayClub);
    }

    private BoardButtonStatus getButtonStatus(User user, Board board, Optional<Enroll> enrollOptional) {
        // 내가 작성한 글인 경우
        if (board.getUser().getId().equals(user.getId())) {
            return BoardButtonStatus.VIEW_CHAT;
        }

        // 신청 기록이 없는 경우
        if (enrollOptional.isEmpty()) {
            return BoardButtonStatus.APPLY;
        }

        // 신청 기록이 있는 경우 상태에 따라 분기
        Enroll enroll = enrollOptional.get();
        return switch (enroll.getAcceptStatus()) {
            case ACCEPTED -> BoardButtonStatus.VIEW_CHAT;
            case PENDING -> BoardButtonStatus.CANCEL;
            case REJECTED -> BoardButtonStatus.REJECTED;
            default -> BoardButtonStatus.APPLY;
        };
    }
}
