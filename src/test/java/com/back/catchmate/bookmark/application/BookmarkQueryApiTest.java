package com.back.catchmate.bookmark.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.bookmark.domain.BookmarkRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkQueryApiTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkQueryApi bookmarkQueryApi;

    @Test
    @DisplayName("찜한 게시글 ID 를 고르고, 입력이 비면 조회하지 않는다")
    void getBookmarkedBoardIds() {
        // given
        given(bookmarkRepository.findBookmarkedBoardIds(1L, List.of(10L, 11L))).willReturn(Set.of(11L));

        // when & then
        assertThat(bookmarkQueryApi.getBookmarkedBoardIds(1L, List.of(10L, 11L)))
                .containsExactly(11L);
        assertThat(bookmarkQueryApi.getBookmarkedBoardIds(1L, List.of())).isEmpty();
        then(bookmarkRepository).should().findBookmarkedBoardIds(1L, List.of(10L, 11L));
    }
}
