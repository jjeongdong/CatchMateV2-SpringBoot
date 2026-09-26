package com.back.catchmate.user.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.user.domain.exception.BlockSelfNotAllowedException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// (blocker_id, blocked_id) 유니크 제약이 "이미 차단함" 규칙의 최종 방어선이다 (BlockRepository.save 참고).
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "blocks",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})})
public class Block extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    private Block(Long blockerId, Long blockedId) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
    }

    public static Block create(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BlockSelfNotAllowedException();
        }
        return new Block(blockerId, blockedId);
    }
}
