package com.back.catchmate.bookmark.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.bookmark.domain.Bookmark;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 운영 RDS 를 가리키는 dev 프로필을 끄고 컨테이너 DB 에만 붙는다 (BoardRepositoryImplQueryTest 와 같은 이유).
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, BookmarkRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class BookmarkRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private BookmarkRepositoryImpl bookmarkRepository;

    @Test
    @DisplayName("같은 게시글을 두 번 찜해도 한 행이고, 찜 취소는 없어도 조용히 끝난다")
    void idempotentSaveAndDelete() {
        // when
        bookmarkRepository.saveIfAbsent(Bookmark.create(1L, 10L));
        bookmarkRepository.saveIfAbsent(Bookmark.create(1L, 10L));

        // then
        assertThat(bookmarkRepository.countByUserId(1L)).isEqualTo(1);
        bookmarkRepository.deleteByUserIdAndBoardId(1L, 10L);
        bookmarkRepository.deleteByUserIdAndBoardId(1L, 10L);
        assertThat(bookmarkRepository.existsByUserIdAndBoardId(1L, 10L)).isFalse();
    }

    @Test
    @DisplayName("찜 목록은 최근 찜한 순이고, 주어진 게시글 중 찜한 것만 고른다")
    void listsLatestFirst() {
        // given
        bookmarkRepository.saveIfAbsent(Bookmark.create(1L, 10L));
        bookmarkRepository.saveIfAbsent(Bookmark.create(1L, 11L));
        bookmarkRepository.saveIfAbsent(Bookmark.create(2L, 12L));

        // when
        List<Bookmark> page = bookmarkRepository.findAllByUserId(1L, 0, 10);

        // then
        assertThat(page).extracting(Bookmark::getBoardId).containsExactly(11L, 10L);
        assertThat(bookmarkRepository.findBookmarkedBoardIds(1L, List.of(10L, 12L)))
                .containsExactly(10L);
    }
}
