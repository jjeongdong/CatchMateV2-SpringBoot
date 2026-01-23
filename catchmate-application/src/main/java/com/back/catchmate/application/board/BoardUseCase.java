package com.back.catchmate.application.board;

import com.back.catchmate.application.board.dto.command.BoardCreateCommand;
import com.back.catchmate.application.board.dto.response.BoardDetailResponse;
import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.application.board.dto.response.BoardTempResponse;
import com.back.catchmate.application.common.PagedResponse;
import com.back.catchmate.domain.board.dto.BoardSearchCondition;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.common.DomainPage;
import com.back.catchmate.domain.common.DomainPageable;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.service.GameService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Transactional
    public BoardResponse writeBoard(Long userId, BoardCreateCommand command) {
        if (command.getBoardId() != null) {
            return updateBoard(command.getBoardId(), userId, command);
        }

        Optional<Board> oldDraft = boardService.findUncompletedBoard(userId);
        oldDraft.ifPresent(boardService::deleteBoard);
        return createBoard(userId, command);
    }

    private BoardResponse createBoard(Long userId, BoardCreateCommand command) {
        User user = userService.getUserById(userId);
        Club cheerClub = clubService.getClub(command.getCheerClubId());

        Club homeClub = clubService.getClub(command.getGameCreateCommand().getHomeClubId());
        Club awayClub = clubService.getClub(command.getGameCreateCommand().getAwayClubId());

        // 게임 생성
        Game game = Game.createGame(
                homeClub,
                awayClub,
                command.getGameCreateCommand().getGameStartDate(),
                command.getGameCreateCommand().getLocation()
        );
        Game savedGame = gameService.createGame(game);

        // 게시글 도메인 객체 생성
        Board board = Board.createBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                user,
                cheerClub,
                savedGame,
                command.getPreferredGender(),
                command.getPreferredAgeRange(),
                command.isCompleted()
        );

        // 게시글 저장
        Board savedBoard = boardService.createBoard(board);
        return BoardResponse.of(savedBoard, false);
    }

    private BoardResponse updateBoard(Long boardId, Long userId, BoardCreateCommand command) {
        // 게시글 조회
        Board board = boardService.getBoard(boardId);

        // 권한 체크
        if (!board.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 변경할 Club 정보들 조회 (응원 구단, 홈 구단, 원정 구단)
        Club cheerClub = clubService.getClub(command.getCheerClubId());
        Club homeClub = clubService.getClub(command.getGameCreateCommand().getHomeClubId());
        Club awayClub = clubService.getClub(command.getGameCreateCommand().getAwayClubId());

        // Game 정보 수정 (홈/원정 팀 + 날짜 + 장소)
        Game game = board.getGame();
        game.update(
                homeClub,
                awayClub,
                command.getGameCreateCommand().getGameStartDate(),
                command.getGameCreateCommand().getLocation()
        );

        // Board 도메인 모델 업데이트 (응원팀 + 나머지 정보)
        board.updateBoard(
                command.getTitle(),
                command.getContent(),
                command.getMaxPerson(),
                cheerClub,
                command.getPreferredGender(),
                command.getPreferredAgeRange(),
                command.isCompleted()
        );

        // 6. 변경사항 저장
        boardService.updateBoard(board);
        return BoardResponse.of(board, false);
    }

    public BoardDetailResponse getBoard(Long userId, Long boardId) {
        // 게시글 도메인 조회 (없으면 예외 발생)
        Board board = boardService.getBoard(boardId);

        // 찜 여부 확인
        // TODO: BookmarkService 연동 필요
        boolean isBookMarked = false;

        // 버튼 상태 계산 (작성자 여부, 신청 여부 등 확인)
        String buttonStatus = getButtonStatus(userId, board);

        // 채팅방 ID 조회
        // TODO: ChatService 연동 필요
        Long chatRoomId = null;

        return BoardDetailResponse.of(board, isBookMarked, buttonStatus, chatRoomId);
    }

    private String getButtonStatus(Long userId, Board board) {
        // 작성자 본인인 경우
        if (board.getUser().getId().equals(userId)) {
            return "VIEW_CHAT";
        }
        // TODO: 로직 추가 필요
        return "APPLY";
    }

    public PagedResponse<BoardResponse> getBoardList(Long userId, LocalDate gameDate, Integer maxPerson,
                                                     List<Long> preferredTeamIdList, int page, int size) {

        // TODO 차단된 유저 목록 조회 (현재는 빈 리스트로 처리)
        List<Long> blockedUserIds = Collections.emptyList();

        // 검색 조건 생성
        BoardSearchCondition condition = BoardSearchCondition.builder()
                .userId(userId)
                .gameDate(gameDate)
                .maxPerson(maxPerson)
                .preferredTeamIdList(preferredTeamIdList)
                .blockedUserIds(blockedUserIds)
                .build();

        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);
        DomainPage<Board> boardPage = boardService.getBoardList(condition, domainPageable);

        List<BoardResponse> boardResponses = boardPage.getContent().stream()
                .map(board -> BoardResponse.of(board, false))
                .toList();

        return new PagedResponse<>(boardPage, boardResponses);
    }

    public PagedResponse<BoardResponse> getBoardsByUserId(Long targetUserId, Long loginUserId, int page, int size) {
        // 도메인 페이징 객체 생성
        DomainPageable domainPageable = DomainPageable.of(page, size);

        // 서비스 호출
        DomainPage<Board> boardPage = boardService.getBoardsByUserId(targetUserId, domainPageable);

        // 응답 DTO 변환
        List<BoardResponse> responses = boardPage.getContent().stream()
                .map(board -> {
                    // TODO: 찜 여부 확인
                    boolean isBookMarked = false;
                    return BoardResponse.of(board, isBookMarked);
                })
                .toList();

        return new PagedResponse<>(boardPage, responses);
    }

    public BoardTempResponse getTempBoard(Long userId) {
        Optional<Board> tempBoard = boardService.getTempBoard(userId);
        return tempBoard.map(BoardTempResponse::from).orElse(null);
    }
}
