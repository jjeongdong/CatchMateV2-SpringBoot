package com.back.catchmate.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.exception.BlockAlreadyExistsException;
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

// UserRepositoryImplQueryTest 와 같은 이유로 test 프로필 + 컨테이너 datasource 만 쓴다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QuerydslConfig.class, JpaAuditingConfig.class, BlockRepositoryImpl.class})
@Testcontainers(disabledWithoutDocker = true)
class BlockRepositoryImplQueryTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private BlockRepositoryImpl blockRepository;

    @Test
    @DisplayName("차단 목록은 최신순으로 offset/limit 을 적용하고 전체 수를 센다")
    void pagesByBlocker() {
        // given
        Block first = blockRepository.save(Block.create(1L, 2L));
        Block second = blockRepository.save(Block.create(1L, 3L));
        Block third = blockRepository.save(Block.create(1L, 4L));
        blockRepository.save(Block.create(9L, 2L));

        // when
        List<Block> firstPage = blockRepository.findAllByBlockerId(1L, 0, 2);
        List<Block> secondPage = blockRepository.findAllByBlockerId(1L, 2, 2);

        // then
        assertThat(firstPage).extracting(Block::getId).containsExactly(third.getId(), second.getId());
        assertThat(secondPage).extracting(Block::getId).containsExactly(first.getId());
        assertThat(blockRepository.countByBlockerId(1L)).isEqualTo(3);
        assertThat(blockRepository.findBlockedIdsByBlockerId(1L)).containsExactlyInAnyOrder(2L, 3L, 4L);
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(1L, 2L)).isTrue();
        assertThat(blockRepository.existsByBlockerIdAndBlockedId(2L, 1L)).isFalse();
    }

    @Test
    @DisplayName("같은 차단을 두 번 저장하면 실제 유니크 제약이 BlockAlreadyExistsException 으로 바뀐다")
    void rejectsDuplicateByUniqueConstraint() {
        // given
        blockRepository.save(Block.create(1L, 2L));

        // when & then
        assertThatThrownBy(() -> blockRepository.save(Block.create(1L, 2L)))
                .isInstanceOf(BlockAlreadyExistsException.class);
    }
}
