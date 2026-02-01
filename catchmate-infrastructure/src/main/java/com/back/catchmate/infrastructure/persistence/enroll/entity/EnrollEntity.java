package com.back.catchmate.infrastructure.persistence.enroll.entity;

import com.back.catchmate.domain.enroll.model.AcceptStatus;
import com.back.catchmate.domain.enroll.model.Enroll;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

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
                .user(UserEntity.from(enroll.getUser()))
                .board(BoardEntity.from(enroll.getBoard()))
                .description(enroll.getDescription())
                .acceptStatus(enroll.getAcceptStatus())
                .newEnroll(enroll.isNewEnroll())
                .build();
    }

    public Enroll toModel() {
        return Enroll.builder()
                .id(id)
                .user(user.toModel())
                .board(board.toModel())
                .description(description)
                .acceptStatus(acceptStatus)
                .newEnroll(newEnroll)
                .requestedAt(getCreatedAt())
                .build();
    }
}
