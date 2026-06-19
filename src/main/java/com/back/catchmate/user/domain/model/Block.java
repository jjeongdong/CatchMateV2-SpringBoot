package com.back.catchmate.user.domain.model;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
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
    private Long blockerId;
    private Long blockedId;

    public static Block createBlock(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BaseException(ErrorCode.SELF_BLOCK_FAILED);
        }
        return Block.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
    }
}
