package com.back.catchmate.domain.user.model;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Block {
    private Long id;
    private User blocker; // 차단한 사람
    private User blocked; // 차단당한 사람

    public static Block createBlock(User blocker, User blocked) {
        return Block.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
    }
}
