package com.back.catchmate.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.back.catchmate.user.domain.exception.BlockSelfNotAllowedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlockTest {

    @Test
    @DisplayName("차단한 사람과 차단된 사람을 담아 생성한다")
    void create() {
        // when
        Block block = Block.create(1L, 2L);

        // then
        assertThat(block.getBlockerId()).isEqualTo(1L);
        assertThat(block.getBlockedId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("자기 자신은 차단할 수 없다")
    void rejectsSelfBlock() {
        assertThatThrownBy(() -> Block.create(1L, 1L)).isInstanceOf(BlockSelfNotAllowedException.class);
    }
}
