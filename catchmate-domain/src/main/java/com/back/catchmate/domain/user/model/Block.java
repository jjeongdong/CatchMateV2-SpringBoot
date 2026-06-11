package com.back.catchmate.domain.user.model;

import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Block {
    private Long id;
    private User blocker; // 차단한 사람
    private User blocked; // 차단당한 사람

    public static Block createBlock(User blocker, User blocked) {
        validateNotSelfBlock(blocker, blocked);

        return Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
    }

    private static void validateNotSelfBlock(User blocker, User blocked) {
        if (Objects.equals(blocker.getId(), blocked.getId())) {
            throw new BaseException(ErrorCode.SELF_BLOCK_FAILED);
        }
    }
}
