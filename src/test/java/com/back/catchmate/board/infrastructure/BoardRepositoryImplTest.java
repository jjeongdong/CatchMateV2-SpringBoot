package com.back.catchmate.board.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.board.domain.exception.BoardNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoardRepositoryImplTest {

    @Mock
    private BoardJpaRepository boardJpaRepository;

    @Mock
    private JPAQueryFactory jpaQueryFactory;

    @InjectMocks
    private BoardRepositoryImpl boardRepository;

    @Test
    @DisplayName("없는 게시글을 getById 하면 BoardNotFoundException")
    void getByIdThrows() {
        given(boardJpaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardRepository.getById(1L)).isInstanceOf(BoardNotFoundException.class);
    }

    @Test
    @DisplayName("발행 글이 아니면 getPublishedById 는 BoardNotFoundException")
    void getPublishedByIdThrows() {
        given(boardJpaRepository.findByIdAndCompletedTrue(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boardRepository.getPublishedById(1L)).isInstanceOf(BoardNotFoundException.class);
    }
}
