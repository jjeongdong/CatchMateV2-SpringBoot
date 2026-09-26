package com.back.catchmate.enroll.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.fixture.BoardFixture;
import com.back.catchmate.bookmark.application.BookmarkQueryApi;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.enroll.application.dto.result.EnrollApplicantResult;
import com.back.catchmate.enroll.application.dto.result.EnrollDetailResult;
import com.back.catchmate.enroll.application.dto.result.EnrollReceivedResult;
import com.back.catchmate.enroll.application.dto.result.EnrollRequestResult;
import com.back.catchmate.enroll.domain.Enroll;
import com.back.catchmate.enroll.domain.EnrollRepository;
import com.back.catchmate.enroll.domain.exception.EnrollNotBoardWriterException;
import com.back.catchmate.enroll.domain.exception.EnrollNotParticipantException;
import com.back.catchmate.enroll.fixture.EnrollFixture;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollQueryServiceTest {

    private static final Long APPLICANT = 1L;
    private static final Long WRITER = 2L;
    private static final Long BOARD = 10L;

    @Mock
    private EnrollRepository enrollRepository;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private BookmarkQueryApi bookmarkQueryApi;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private EnrollQueryService enrollQueryService;

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

    // 게시글 요약 조립에 쓰이는 작성자·경기·구단 조회를 비워 둔다.
    private void stubBoardReferences() {
        given(userQueryApi.getInfos(List.of(WRITER))).willReturn(Map.of(WRITER, user(WRITER, null)));
        given(gameQueryApi.getInfos(any())).willReturn(Map.of());
        given(clubQueryApi.getInfos(any())).willReturn(Map.of());
    }

    @Test
    @DisplayName("신청자도 작성자도 아니면 상세를 볼 수 없고 게시글도 조회하지 않는다")
    void getEnrollRejectsStranger() {
        given(enrollRepository.getById(100L)).willReturn(EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER));

        assertThatThrownBy(() -> enrollQueryService.getEnroll(3L, 100L))
                .isInstanceOf(EnrollNotParticipantException.class);
        then(boardQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("작성자가 상세를 보면 신청자와 게시글 요약을 담고, 신청자 구단이 없으면 구단 단건 조회를 하지 않는다")
    void getEnroll() {
        // given
        given(enrollRepository.getById(100L)).willReturn(EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER));
        given(boardQueryApi.getInfo(BOARD)).willReturn(BoardFixture.info(BOARD, WRITER));
        given(userQueryApi.getInfo(APPLICANT)).willReturn(user(APPLICANT, null));
        stubBoardReferences();

        // when
        EnrollDetailResult result = enrollQueryService.getEnroll(WRITER, 100L);

        // then
        assertThat(result.applicant().nickName()).isEqualTo("닉1");
        assertThat(result.boardResponse().boardId()).isEqualTo(BOARD);
        assertThat(result.boardResponse().bookMarked()).isFalse();
        then(clubQueryApi).should(never()).getInfo(any());
    }

    @Test
    @DisplayName("보낸 신청이 없으면 게시글·찜을 조회하지 않는다")
    void getMyEnrollsEmpty() {
        given(enrollRepository.findAllByApplicantId(APPLICANT, 0L, 20)).willReturn(List.of());
        given(enrollRepository.countByApplicantId(APPLICANT)).willReturn(0L);

        OffsetPageResult<EnrollRequestResult> result = enrollQueryService.getMyEnrolls(APPLICANT, 0, 20);

        assertThat(result.content()).isEmpty();
        then(boardQueryApi).shouldHaveNoInteractions();
        then(bookmarkQueryApi).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("같은 게시글 신청이 여러 건이어도 게시글은 한 번만 조회하고 찜 여부를 반영한다")
    void getMyEnrolls() {
        // given
        Enroll first = EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER);
        Enroll second = EnrollFixture.pending(101L, APPLICANT, BOARD, WRITER);
        given(enrollRepository.findAllByApplicantId(APPLICANT, 0L, 20)).willReturn(List.of(first, second));
        given(enrollRepository.countByApplicantId(APPLICANT)).willReturn(2L);
        given(bookmarkQueryApi.getBookmarkedBoardIds(APPLICANT, List.of(BOARD))).willReturn(Set.of(BOARD));
        given(boardQueryApi.getInfos(List.of(BOARD))).willReturn(Map.of(BOARD, BoardFixture.info(BOARD, WRITER)));
        stubBoardReferences();

        // when
        OffsetPageResult<EnrollRequestResult> result = enrollQueryService.getMyEnrolls(APPLICANT, 0, 20);

        // then
        assertThat(result.content()).hasSize(2).allSatisfy(item -> {
            assertThat(item.boardResponse().boardId()).isEqualTo(BOARD);
            assertThat(item.boardResponse().bookMarked()).isTrue();
        });
    }

    @Test
    @DisplayName("내 게시글이 아니면 신청자 목록을 볼 수 없다")
    void getBoardEnrollsRejectsOther() {
        given(boardQueryApi.getInfo(BOARD)).willReturn(BoardFixture.info(BOARD, WRITER));

        assertThatThrownBy(() -> enrollQueryService.getBoardEnrolls(APPLICANT, BOARD, 0, 20))
                .isInstanceOf(EnrollNotBoardWriterException.class);
        then(enrollRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("신청자 목록의 새 신청 표시는 실제 값을 따른다")
    void getBoardEnrolls() {
        // given
        Enroll read = EnrollFixture.pending(100L, APPLICANT, BOARD, WRITER);
        read.markAsRead(WRITER);
        given(boardQueryApi.getInfo(BOARD)).willReturn(BoardFixture.info(BOARD, WRITER));
        given(enrollRepository.findPendingByBoardId(BOARD, 0L, 20)).willReturn(List.of(read));
        given(enrollRepository.countPendingByBoardId(BOARD)).willReturn(1L);
        given(userQueryApi.getInfos(List.of(APPLICANT))).willReturn(Map.of(APPLICANT, user(APPLICANT, null)));

        // when
        OffsetPageResult<EnrollApplicantResult> result = enrollQueryService.getBoardEnrolls(WRITER, BOARD, 0, 20);

        // then
        assertThat(result.content()).singleElement().satisfies(item -> {
            assertThat(item.newEnroll()).isFalse();
            assertThat(item.applicantResponse().nickname()).isEqualTo("닉1");
        });
    }

    @Test
    @DisplayName("받은 신청은 게시글별로 자기 신청만 묶고, 신청이 없는 게시글은 뺀다")
    void getReceivedEnrolls() {
        // given
        Long emptyBoard = 11L;
        given(enrollRepository.findBoardIdsWithPendingByOwnerId(WRITER, 0L, 20)).willReturn(List.of(BOARD, emptyBoard));
        given(enrollRepository.countBoardsWithPendingByOwnerId(WRITER)).willReturn(2L);
        given(enrollRepository.findPendingByBoardIds(List.of(BOARD, emptyBoard)))
                .willReturn(List.of(
                        EnrollFixture.pending(100L, 1L, BOARD, WRITER),
                        EnrollFixture.pending(101L, 3L, BOARD, WRITER)));
        given(boardQueryApi.getInfos(List.of(BOARD, emptyBoard)))
                .willReturn(Map.of(
                        BOARD, BoardFixture.info(BOARD, WRITER), emptyBoard, BoardFixture.info(emptyBoard, WRITER)));
        given(userQueryApi.getInfos(List.of(1L, 3L))).willReturn(Map.of(1L, user(1L, null), 3L, user(3L, null)));
        stubBoardReferences();

        // when
        OffsetPageResult<EnrollReceivedResult> result = enrollQueryService.getReceivedEnrolls(WRITER, 0, 20);

        // then
        assertThat(result.content()).singleElement().satisfies(received -> {
            assertThat(received.boardResponse().boardId()).isEqualTo(BOARD);
            assertThat(received.enrollResponses())
                    .extracting(EnrollReceivedResult.EnrollView::enrollId)
                    .containsExactly(100L, 101L);
        });
        assertThat(result.totalElements()).isEqualTo(2L);
    }
}
