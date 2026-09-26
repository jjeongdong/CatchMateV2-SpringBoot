package com.back.catchmate.bookmark.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.board.application.BoardQueryApi;
import com.back.catchmate.board.fixture.BoardFixture;
import com.back.catchmate.bookmark.application.dto.result.BookmarkedBoardResult;
import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.bookmark.domain.BookmarkRepository;
import com.back.catchmate.club.application.ClubQueryApi;
import com.back.catchmate.club.application.dto.api.ClubInfo;
import com.back.catchmate.game.application.GameQueryApi;
import com.back.catchmate.game.application.dto.api.GameInfo;
import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkQueryServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BoardQueryApi boardQueryApi;

    @Mock
    private UserQueryApi userQueryApi;

    @Mock
    private GameQueryApi gameQueryApi;

    @Mock
    private ClubQueryApi clubQueryApi;

    @InjectMocks
    private BookmarkQueryService bookmarkQueryService;

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

    @Test
    @DisplayName("찜한 순서대로 조립하고, 삭제된 게시글은 빠진다")
    void getBookmarks() {
        // given (최근 찜: 11 → 99(삭제됨) → 10)
        given(bookmarkRepository.findAllByUserId(1L, 0L, 20))
                .willReturn(List.of(Bookmark.create(1L, 11L), Bookmark.create(1L, 99L), Bookmark.create(1L, 10L)));
        given(bookmarkRepository.countByUserId(1L)).willReturn(3L);
        given(boardQueryApi.getInfos(List.of(11L, 99L, 10L)))
                .willReturn(Map.of(10L, BoardFixture.info(10L, 2L), 11L, BoardFixture.info(11L, 2L)));
        given(userQueryApi.getInfos(List.of(2L))).willReturn(Map.of(2L, user(2L, 1L)));
        given(gameQueryApi.getInfos(List.of(100L)))
                .willReturn(Map.of(100L, new GameInfo(100L, LocalDateTime.of(2026, 10, 1, 18, 30), "잠실", 1L, 2L)));
        given(clubQueryApi.getInfos(any()))
                .willReturn(Map.of(1L, new ClubInfo(1L, "LG", "잠실", "서울"), 2L, new ClubInfo(2L, "두산", "잠실", "서울")));

        // when
        OffsetPageResult<BookmarkedBoardResult> result = bookmarkQueryService.getBookmarks(1L, 0, 20);

        // then
        assertThat(result.content()).extracting(BookmarkedBoardResult::boardId).containsExactly(11L, 10L);
        BookmarkedBoardResult first = result.content().get(0);
        assertThat(first.bookMarked()).isTrue();
        assertThat(first.cheerClub().name()).isEqualTo("LG");
        assertThat(first.gameInfo().awayClubName()).isEqualTo("두산");
        assertThat(first.userInfo().clubName()).isEqualTo("LG");
        assertThat(result.totalElements()).isEqualTo(3L);
    }

    @Test
    @DisplayName("찜이 없으면 게시글을 조회하지 않는다")
    void getBookmarksEmpty() {
        // given
        given(bookmarkRepository.findAllByUserId(1L, 0L, 20)).willReturn(List.of());
        given(bookmarkRepository.countByUserId(1L)).willReturn(0L);

        // when
        OffsetPageResult<BookmarkedBoardResult> result = bookmarkQueryService.getBookmarks(1L, 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        then(boardQueryApi).shouldHaveNoInteractions();
    }
}
