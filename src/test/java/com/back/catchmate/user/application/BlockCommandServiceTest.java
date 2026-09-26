package com.back.catchmate.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.user.application.dto.command.BlockCreateCommand;
import com.back.catchmate.user.application.dto.result.BlockCreateResult;
import com.back.catchmate.user.domain.Block;
import com.back.catchmate.user.domain.BlockRepository;
import com.back.catchmate.user.domain.UserRepository;
import com.back.catchmate.user.domain.event.UserBlockedEvent;
import com.back.catchmate.user.domain.exception.BlockAlreadyExistsException;
import com.back.catchmate.user.domain.exception.UserNotFoundException;
import com.back.catchmate.user.fixture.BlockFixture;
import com.back.catchmate.user.fixture.UserFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BlockCommandServiceTest {

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BlockCommandService blockCommandService;

    @Test
    @DisplayName("차단하면 저장하고 UserBlockedEvent 를 발행한다")
    void createBlockPublishesEvent() {
        // given
        given(userRepository.getById(2L)).willReturn(UserFixture.user(2L, 3L));
        given(blockRepository.save(any(Block.class))).willReturn(BlockFixture.block(10L, 1L, 2L));

        // when
        BlockCreateResult result = blockCommandService.createBlock(1L, new BlockCreateCommand(2L));

        // then
        assertThat(result).isEqualTo(new BlockCreateResult(10L, 2L));
        then(eventPublisher).should().publishEvent(new UserBlockedEvent(1L, 2L));
    }

    @Test
    @DisplayName("이미 차단했으면 예외가 전파되고 이벤트를 발행하지 않는다")
    void duplicateDoesNotPublish() {
        // given
        given(userRepository.getById(2L)).willReturn(UserFixture.user(2L, 3L));
        given(blockRepository.save(any(Block.class))).willThrow(new BlockAlreadyExistsException());

        // when & then
        assertThatThrownBy(() -> blockCommandService.createBlock(1L, new BlockCreateCommand(2L)))
                .isInstanceOf(BlockAlreadyExistsException.class);
        then(eventPublisher).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("존재하지 않는 유저는 차단할 수 없다")
    void throwsWhenBlockedUserMissing() {
        // given
        given(userRepository.getById(99L)).willThrow(new UserNotFoundException());

        // when & then
        assertThatThrownBy(() -> blockCommandService.createBlock(1L, new BlockCreateCommand(99L)))
                .isInstanceOf(UserNotFoundException.class);
        then(blockRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("차단 해제는 찾은 차단 내역을 지운다")
    void deleteBlock() {
        // given
        Block block = BlockFixture.block(10L, 1L, 2L);
        given(blockRepository.getByBlockerIdAndBlockedId(1L, 2L)).willReturn(block);

        // when
        blockCommandService.deleteBlock(1L, 2L);

        // then
        then(blockRepository).should().delete(block);
    }
}
