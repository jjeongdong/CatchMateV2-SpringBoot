package com.back.catchmate.user.adapter.out.persistence.entity;

import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
        })
public class BlockEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    public static BlockEntity from(Block block) {
        return BlockEntity.builder()
                .id(block.getId())
                .blockerId(block.getBlockerId())
                .blockedId(block.getBlockedId())
                .build();
    }

    public Block toDomain() {
        return Block.builder()
                .id(this.id)
                .blockerId(this.blockerId)
                .blockedId(this.blockedId)
                .build();
    }
}
