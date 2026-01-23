package com.back.catchmate.application.board;

import com.back.catchmate.application.board.dto.command.BoardCreateCommand;
import com.back.catchmate.application.board.dto.response.BoardResponse;
import com.back.catchmate.domain.board.model.Board;
import com.back.catchmate.domain.board.service.BoardService;
import com.back.catchmate.domain.club.model.Club;
import com.back.catchmate.domain.club.service.ClubService;
import com.back.catchmate.domain.game.model.Game;
import com.back.catchmate.domain.game.service.GameService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
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
        return BoardResponse.of(savedBoard, false, "buttonSample", null);
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
        return BoardResponse.of(board, false, "buttonSample", null);
    }
}
