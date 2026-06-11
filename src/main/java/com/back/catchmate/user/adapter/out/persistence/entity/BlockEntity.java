package com.back.catchmate.user.adapter.out.persistence.entity;

import com.back.catchmate.user.domain.model.Block;
import com.back.catchmate.global.infrastructure.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "blocks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"blocker_id", "blocked_id"})
        })
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BlockEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private UserEntity blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", nullable = false)
    private UserEntity blocked;

    public static BlockEntity from(Block block) {
        return BlockEntity.builder()
                .id(block.getId())
                .blocker(UserEntity.from(block.getBlocker()))
                .blocked(UserEntity.from(block.getBlocked()))
                .build();
    }

    public Block toModel() {
        return Block.builder()
                .id(this.id)
                .blocker(this.blocker.toModel())
                .blocked(this.blocked.toModel())
                .build();
    }
}
