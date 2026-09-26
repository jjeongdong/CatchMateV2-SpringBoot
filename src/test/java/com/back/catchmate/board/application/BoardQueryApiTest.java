package com.back.catchmate.board.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.board.application.dto.api.BoardInfo;
import com.back.catchmate.board.domain.BoardRepository;
import com.back.catchmate.board.domain.exception.BoardNotFoundException;
import com.back.catchmate.board.fixture.BoardFixture;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardQueryApiTest {

    @Mock
    private BoardRepository boardRepository;

    @InjectMocks
    private BoardQueryApi boardQueryApi;

    @Test
    @DisplayName("여러 게시글을 ID 로 묶어 돌려주고, 입력이 비면 조회하지 않는다")
    void getInfos() {
        // given
        given(boardRepository.findAllByIds(List.of(10L, 11L)))
                .willReturn(List.of(BoardFixture.published(10L, 1L), BoardFixture.published(11L, 2L)));

        // when
        Map<Long, BoardInfo> result = boardQueryApi.getInfos(List.of(10L, 11L));

        // then
        assertThat(result).containsOnlyKeys(10L, 11L);
        assertThat(result.get(11L).userId()).isEqualTo(2L);
        assertThat(boardQueryApi.getInfos(List.of())).isEmpty();
        then(boardRepository).should().findAllByIds(List.of(10L, 11L));
    }

    @Test
    @DisplayName("발행 글이 아니면 getPublishedInfo 는 BoardNotFoundException 을 그대로 던진다")
    void getPublishedInfoPropagates() {
        given(boardRepository.getPublishedById(10L)).willThrow(new BoardNotFoundException());

        assertThatThrownBy(() -> boardQueryApi.getPublishedInfo(10L)).isInstanceOf(BoardNotFoundException.class);
    }
}
