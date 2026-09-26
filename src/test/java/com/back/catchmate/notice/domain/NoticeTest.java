package com.back.catchmate.notice.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NoticeTest {

    @Test
    @DisplayName("작성자·제목·내용으로 생성한다")
    void create() {
        // when
        Notice notice = Notice.create(1L, "제목", "내용");

        // then
        assertThat(notice.getWriterId()).isEqualTo(1L);
        assertThat(notice.getTitle()).isEqualTo("제목");
        assertThat(notice.getContent()).isEqualTo("내용");
    }

    @Test
    @DisplayName("제목과 내용을 고친다")
    void revise() {
        // given
        Notice notice = Notice.create(1L, "제목", "내용");

        // when
        notice.revise("새 제목", "새 내용");

        // then
        assertThat(notice.getTitle()).isEqualTo("새 제목");
        assertThat(notice.getContent()).isEqualTo("새 내용");
    }
}
