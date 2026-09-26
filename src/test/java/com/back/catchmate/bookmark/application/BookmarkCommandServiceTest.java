package com.back.catchmate.bookmark.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.bookmark.domain.BookmarkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BookmarkCommandServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private BookmarkCommandService bookmarkCommandService;

    @Test
    @DisplayName("찜하기는 없을 때만 저장하도록 리포지토리에 맡긴다")
    void createBookmark() {
        // when
        bookmarkCommandService.createBookmark(1L, 10L);

        // then
        ArgumentCaptor<Bookmark> bookmark = ArgumentCaptor.forClass(Bookmark.class);
        then(bookmarkRepository).should().saveIfAbsent(bookmark.capture());
        assertThat(bookmark.getValue().getUserId()).isEqualTo(1L);
        assertThat(bookmark.getValue().getBoardId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("찜 취소는 사용자·게시글로 지운다")
    void deleteBookmark() {
        bookmarkCommandService.deleteBookmark(1L, 10L);

        then(bookmarkRepository).should().deleteByUserIdAndBoardId(1L, 10L);
    }
}
