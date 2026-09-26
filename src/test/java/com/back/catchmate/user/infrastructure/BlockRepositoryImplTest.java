package com.back.catchmate.user.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.exception.BlockAlreadyExistsException;
import com.back.catchmate.user.domain.exception.BlockNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class BlockRepositoryImplTest {

    @Mock
    private BlockJpaRepository blockJpaRepository;

    @InjectMocks
    private BlockRepositoryImpl blockRepository;

    @Test
    @DisplayName("유니크 제약 위반은 BlockAlreadyExistsException 으로 바꾼다")
    void translatesUniqueViolation() {
        // given
        Block block = Block.create(1L, 2L);
        given(blockJpaRepository.saveAndFlush(block)).willThrow(new DataIntegrityViolationException("duplicate"));

        // when & then
        assertThatThrownBy(() -> blockRepository.save(block)).isInstanceOf(BlockAlreadyExistsException.class);
    }

    @Test
    @DisplayName("차단 내역이 없으면 BlockNotFoundException 을 던진다")
    void getThrowsWhenMissing() {
        // given
        given(blockJpaRepository.findByBlockerIdAndBlockedId(1L, 2L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> blockRepository.getByBlockerIdAndBlockedId(1L, 2L))
                .isInstanceOf(BlockNotFoundException.class);
    }
}
