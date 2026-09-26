package com.back.catchmate.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.board.application.dto.command.BoardSearchCommand;
import com.back.catchmate.board.application.dto.result.AdminBoardDetailResult;
import com.back.catchmate.board.application.dto.result.AdminBoardResult;
import com.back.catchmate.board.application.dto.result.BoardDetailResult;
import com.back.catchmate.board.application.dto.result.BoardResult;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.BoardSearchCondition;
import com.back.catchmate.board.domain.exception.BoardWriterBlockedException;
import com.back.catchmate.board.fixture.BoardFixture;
import com.back.catchmate.bookmark.application.BookmarkQueryApi;
import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.enroll.application.EnrollQueryApi;
import com.back.catchmate.enroll.application.dto.api.EnrollInfo;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.CursorPageResult;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardQueryServiceTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @Mock
    private EnrollQueryApi enrollQueryApi;

    @Mock
    private BookmarkQueryApi bookmarkQueryApi;

    @Mock
    private ChatQueryApi chatQueryApi;

    @InjectMocks
    private BoardQueryService boardQueryService;

    static UserInfo user(Long userId, Long clubId) {
        return new UserInfo(
                userId,
                "u" + userId + "@catchmate.com",
                null,
                null,
                'M',
                "닉" + userId,
                LocalDate.of(2000, 1, 1),
                "응원형",
                "img-" + userId,
                "ROLE_USER",
                null,
                clubId,
                false,
                false,
                false,
                false,
                null,
                null);
    }

    private void stubReferences(Long writerId) {
        given(userQueryApi.getInfos(List.of(writerId))).willReturn(Map.of(writerId, user(writerId, null)));
        given(gameQueryApi.getInfos(List.of(100L)))
                .willReturn(Map.of(100L, new GameInfo(100L, LocalDateTime.of(2026, 10, 1, 18, 30), "잠실", 1L, 2L)));
        given(clubQueryApi.getInfos(any())).willReturn(Map.of());
    }

    @Test
    @DisplayName("상세는 찜 여부·내 신청·채팅방을 조합하고 대기 중 신청이면 취소 버튼이다")
    void getBoard() {
        // given
        given(boardRepository.getById(10L)).willReturn(BoardFixture.published(10L, 2L));
        given(bookmarkQueryApi.isBookmarked(1L, 10L)).willReturn(true);
        given(enrollQueryApi.findInfo(1L, 10L))
                .willReturn(Optional.of(new EnrollInfo(55L, 1L, 10L, "같이 가요", "PENDING", true, null)));
        given(chatQueryApi.findChatRoomIdByBoardId(10L)).willReturn(Optional.of(7L));
        stubReferences(2L);

        // when
        BoardDetailResult result = boardQueryService.getBoard(1L, 10L);

        // then
        assertThat(result.bookMarked()).isTrue();
        assertThat(result.buttonStatus()).isEqualTo("CANCEL");
        assertThat(result.myEnrollId()).isEqualTo(55L);
        assertThat(result.chatRoomId()).isEqualTo(7L);
        assertThat(result.user().nickName()).isEqualTo("닉2");
        assertThat(result.game().location()).isEqualTo("잠실");
    }

    @Test
    @DisplayName("임시저장 글 상세에서는 채팅방을 찾지 않는다")
    void getDraftBoardSkipsChatRoom() {
        // given
        given(boardRepository.getById(10L)).willReturn(BoardFixture.draft(10L, 1L));
        given(enrollQueryApi.findInfo(1L, 10L)).willReturn(Optional.empty());
        given(userQueryApi.getInfos(List.of(1L))).willReturn(Map.of(1L, user(1L, null)));

        // when
        BoardDetailResult result = boardQueryService.getBoard(1L, 10L);

        // then
        assertThat(result.chatRoomId()).isNull();
        assertThat(result.buttonStatus()).isEqualTo("VIEW_CHAT");
        then(chatQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("날짜 필터에 맞는 경기가 없으면 게시글을 조회하지 않는다")
    void getBoardsShortCircuits() {
        // given
        LocalDate date = LocalDate.of(2026, 10, 1);
        given(userQueryApi.getBlockedUserIds(1L)).willReturn(List.of());
        given(gameQueryApi.getIdsStartingOn(date)).willReturn(List.of());

        // when
        CursorPageResult<BoardResult> result =
                boardQueryService.getBoards(1L, new BoardSearchCommand(date, null, null, null, 20));

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.hasNext()).isFalse();
        then(boardRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("size+1 건을 읽어 다음 페이지가 있으면 마지막 글로 커서를 만든다")
    void getBoardsBuildsNextCursor() {
        // given
        Board b1 = BoardFixture.published(12L, 2L);
        Board b2 = BoardFixture.published(11L, 2L);
        Board b3 = BoardFixture.published(10L, 2L);
        given(userQueryApi.getBlockedUserIds(1L)).willReturn(List.of(9L));
        given(boardRepository.findAllByCondition(any(BoardSearchCondition.class), eq(3)))
                .willReturn(List.of(b1, b2, b3));
        given(bookmarkQueryApi.getBookmarkedBoardIds(1L, List.of(12L, 11L))).willReturn(Set.of(11L));
        stubReferences(2L);

        // when
        CursorPageResult<BoardResult> result =
                boardQueryService.getBoards(1L, new BoardSearchCommand(null, null, null, null, 2));

        // then
        assertThat(result.content()).extracting(BoardResult::boardId).containsExactly(12L, 11L);
        assertThat(result.content()).extracting(BoardResult::bookMarked).containsExactly(false, true);
        assertThat(result.hasNext()).isTrue();
        assertThat(BoardCursor.decode(result.nextCursor())).isEqualTo(new BoardCursor(b2.getLiftUpDate(), 11L));
        ArgumentCaptor<BoardSearchCondition> condition = ArgumentCaptor.forClass(BoardSearchCondition.class);
        then(boardRepository).should().findAllByCondition(condition.capture(), eq(3));
        assertThat(condition.getValue().blockedUserIds()).containsExactly(9L);
        assertThat(condition.getValue().lastBoardId()).isNull();
    }

    @Test
    @DisplayName("다음 커서를 주면 그 정렬 키 뒤부터 읽는다")
    void getBoardsUsesCursor() {
        // given
        BoardCursor cursor = new BoardCursor(LocalDateTime.of(2026, 9, 1, 12, 0), 11L);
        given(userQueryApi.getBlockedUserIds(1L)).willReturn(List.of());
        given(boardRepository.findAllByCondition(any(BoardSearchCondition.class), anyInt()))
                .willReturn(List.of());

        // when
        CursorPageResult<BoardResult> result =
                boardQueryService.getBoards(1L, new BoardSearchCommand(null, null, null, cursor.encode(), 20));

        // then
        assertThat(result.content()).isEmpty();
        ArgumentCaptor<BoardSearchCondition> condition = ArgumentCaptor.forClass(BoardSearchCondition.class);
        then(boardRepository).should().findAllByCondition(condition.capture(), eq(21));
        assertThat(condition.getValue().lastLiftUpDate()).isEqualTo(cursor.liftUpDate());
        assertThat(condition.getValue().lastBoardId()).isEqualTo(11L);
        then(bookmarkQueryApi).shouldHaveNoInteractions();
        then(userQueryApi).should().getBlockedUserIds(1L);
    }

    @Test
    @DisplayName("빈 커서(?cursor=)는 첫 페이지로 읽는다")
    void getBoardsTreatsBlankCursorAsFirstPage() {
        // given
        given(userQueryApi.getBlockedUserIds(1L)).willReturn(List.of());
        given(boardRepository.findAllByCondition(any(BoardSearchCondition.class), anyInt()))
                .willReturn(List.of());

        // when
        CursorPageResult<BoardResult> result =
                boardQueryService.getBoards(1L, new BoardSearchCommand(null, null, null, " ", 20));

        // then
        assertThat(result.content()).isEmpty();
        ArgumentCaptor<BoardSearchCondition> condition = ArgumentCaptor.forClass(BoardSearchCondition.class);
        then(boardRepository).should().findAllByCondition(condition.capture(), eq(21));
        assertThat(condition.getValue().lastBoardId()).isNull();
    }

    @Test
    @DisplayName("내가 차단한 작성자의 게시글 목록은 BoardWriterBlockedException")
    void getUserBoardsRejectsBlockedWriter() {
        // given
        given(userQueryApi.getInfo(2L)).willReturn(user(2L, null));
        given(userQueryApi.isBlocked(1L, 2L)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> boardQueryService.getUserBoards(1L, 2L, null, 20))
                .isInstanceOf(BoardWriterBlockedException.class);
        then(boardRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("관리자 목록은 userId 가 있으면 그 작성자의 글을 센다")
    void getAdminBoardsByWriter() {
        // given
        given(boardRepository.findAllByWriterId(2L, 20L, 10)).willReturn(List.of(BoardFixture.draft(10L, 2L)));
        given(boardRepository.countByWriterId(2L)).willReturn(21L);

        // when
        OffsetPageResult<AdminBoardResult> result = boardQueryService.getAdminBoards(2L, 2, 10);

        // then
        assertThat(result.content()).extracting(AdminBoardResult::boardId).containsExactly(10L);
        assertThat(result.totalElements()).isEqualTo(21L);
        then(boardRepository).should(never()).findAllPublished(anyLong(), anyInt());
    }

    @Test
    @DisplayName("관리자 상세는 대기 신청자와 신청자 구단을 한 번씩 모아 조합한다")
    void getAdminBoard() {
        // given
        given(boardRepository.getPublishedById(10L)).willReturn(BoardFixture.published(10L, 2L));
        given(enrollQueryApi.getPendingInfosByBoardId(10L))
                .willReturn(List.of(new EnrollInfo(55L, 3L, 10L, "d", "PENDING", true, null)));
        given(userQueryApi.getInfos(List.of(2L, 3L))).willReturn(Map.of(2L, user(2L, null), 3L, user(3L, 5L)));
        given(clubQueryApi.getInfos(List.of(5L))).willReturn(Map.of());
        given(gameQueryApi.getInfo(100L))
                .willReturn(new GameInfo(100L, LocalDateTime.of(2026, 10, 1, 18, 30), "잠실", 1L, 2L));

        // when
        AdminBoardDetailResult result = boardQueryService.getAdminBoard(10L);

        // then
        assertThat(result.writerNickname()).isEqualTo("닉2");
        assertThat(result.location()).isEqualTo("잠실");
        assertThat(result.enrollments()).singleElement().satisfies(view -> {
            assertThat(view.enrollId()).isEqualTo(55L);
            assertThat(view.nickName()).isEqualTo("닉3");
            assertThat(view.status()).isEqualTo("PENDING");
        });
    }
}
