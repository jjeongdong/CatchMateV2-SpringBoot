package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.user.application.dto.result.BlockResult;
import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.fixture.BlockFixture;
import com.back.catchmate.user.fixture.UserFixture;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BlockQueryServiceTest {

    private static final LocalDateTime BLOCKED_AT = LocalDateTime.of(2026, 9, 1, 12, 0);

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BlockQueryService blockQueryService;

    private Block block(Long blockId, Long blockedId) {
        Block block = BlockFixture.block(blockId, 1L, blockedId);
        ReflectionTestUtils.setField(block, "createdAt", BLOCKED_AT);
        return block;
    }

    @Test
    @DisplayName("차단 목록에 차단된 유저 정보와 차단 시각을 담고 다음 페이지 여부를 계산한다")
    void combinesUsers() {
        // given
        User blocked = UserFixture.user(2L, 3L);
        given(blockRepository.findAllByBlockerId(1L, 0L, 1)).willReturn(List.of(block(10L, 2L)));
        given(blockRepository.countByBlockerId(1L)).willReturn(2L);
        given(userRepository.findAllByIds(List.of(2L))).willReturn(List.of(blocked));

        // when
        OffsetPageResult<BlockResult> result = blockQueryService.getBlocks(1L, 0, 1);

        // then
        assertThat(result.content())
                .containsExactly(
                        new BlockResult(10L, 2L, blocked.getNickName(), blocked.getProfileImageUrl(), BLOCKED_AT));
        assertThat(result.hasNext()).isTrue();
        assertThat(result.totalElements()).isEqualTo(2L);
    }

    @Test
    @DisplayName("차단된 유저가 탈퇴해 조회되지 않으면 그 항목의 닉네임·이미지는 null 이다")
    void leavesUserFieldsNullWhenBlockedUserMissing() {
        // given
        given(blockRepository.findAllByBlockerId(1L, 0L, 20)).willReturn(List.of(block(10L, 2L)));
        given(blockRepository.countByBlockerId(1L)).willReturn(1L);
        given(userRepository.findAllByIds(List.of(2L))).willReturn(List.of());

        // when
        OffsetPageResult<BlockResult> result = blockQueryService.getBlocks(1L, 0, 20);

        // then
        assertThat(result.content()).containsExactly(new BlockResult(10L, 2L, null, null, BLOCKED_AT));
    }

    @Test
    @DisplayName("차단 내역이 없으면 유저를 조회하지 않는다")
    void skipsUserLookupWhenEmpty() {
        // given
        given(blockRepository.findAllByBlockerId(1L, 0L, 20)).willReturn(List.of());
        given(blockRepository.countByBlockerId(1L)).willReturn(0L);

        // when
        OffsetPageResult<BlockResult> result = blockQueryService.getBlocks(1L, 0, 20);

        // then
        assertThat(result.content()).isEmpty();
        then(userRepository).shouldHaveNoInteractions();
    }
}
