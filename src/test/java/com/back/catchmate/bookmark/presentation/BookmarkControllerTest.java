package com.back.catchmate.bookmark.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.back.catchmate.bookmark.application.BookmarkCommandService;
import com.back.catchmate.bookmark.application.BookmarkQueryService;
import com.back.catchmate.global.authorization.resolver.AuthUserArgumentResolver;
import com.back.catchmate.global.error.GlobalExceptionHandler;
import com.back.catchmate.global.response.OffsetPageResult;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class BookmarkControllerTest {

    @Mock
    private BookmarkCommandService bookmarkCommandService;

    @Mock
    private BookmarkQueryService bookmarkQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new BookmarkController(bookmarkCommandService, bookmarkQueryService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthUserArgumentResolver())
                .build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("1", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("PUT /api/bookmarks/{boardId} 는 204")
    void createBookmark() throws Exception {
        mockMvc.perform(put("/api/bookmarks/10")).andExpect(status().isNoContent());

        then(bookmarkCommandService).should().createBookmark(1L, 10L);
    }

    @Test
    @DisplayName("DELETE /api/bookmarks/{boardId} 는 204")
    void deleteBookmark() throws Exception {
        mockMvc.perform(delete("/api/bookmarks/10")).andExpect(status().isNoContent());

        then(bookmarkCommandService).should().deleteBookmark(1L, 10L);
    }

    @Test
    @DisplayName("GET /api/bookmarks 의 기본 크기는 20")
    void getBookmarksDefaults() throws Exception {
        given(bookmarkQueryService.getBookmarks(1L, 0, 20)).willReturn(OffsetPageResult.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/bookmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }
}
