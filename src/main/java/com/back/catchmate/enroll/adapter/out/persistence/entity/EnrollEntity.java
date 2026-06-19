package com.back.catchmate.enroll.adapter.out.persistence.entity;

import com.back.catchmate.enroll.domain.model.AcceptStatus;
import com.back.catchmate.enroll.domain.model.Enroll;
import com.back.catchmate.global.persistence.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "enrolls",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_enroll_user_board",
                        columnNames = {"user_id", "board_id"}
                )
        }
)
public class EnrollEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "enroll_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    /** 게시글 작성자 ID — 생성 시점 board.userId 스냅샷 (cross-context 조인 회피용). */
    @Column(name = "board_owner_id", nullable = false)
    private Long boardOwnerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcceptStatus acceptStatus;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean newEnroll;

    public static EnrollEntity from(Enroll enroll) {
        return EnrollEntity.builder()
                .id(enroll.getId())
                .userId(enroll.getUserId())
                .boardId(enroll.getBoardId())
                .boardOwnerId(enroll.getBoardOwnerId())
                .description(enroll.getDescription())
                .acceptStatus(enroll.getAcceptStatus())
                .newEnroll(enroll.isNewEnroll())
                .build();
    }

    public Enroll toDomain() {
        return Enroll.builder()
                .id(id)
                .userId(userId)
                .boardId(boardId)
                .boardOwnerId(boardOwnerId)
                .description(description)
                .acceptStatus(acceptStatus)
                .newEnroll(newEnroll)
                .requestedAt(getCreatedAt())
                .build();
    }
}
