package com.back.catchmate.infrastructure.persistence.bookmark.entity;

import com.back.catchmate.domain.bookmark.model.Bookmark;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.board.entity.BoardEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "bookmarks",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "board_id"})
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookmarkEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    private BoardEntity board;

    @Builder
    public BookmarkEntity(Long id, UserEntity user, BoardEntity board) {
        this.id = id;
        this.user = user;
        this.board = board;
    }

    public Bookmark toModel() {
        return Bookmark.builder()
                .id(id)
                .user(user.toModel())
                .board(board.toModel())
                .createdAt(getCreatedAt())
                .build();
    }

    public static BookmarkEntity from(Bookmark bookmark) {
        return BookmarkEntity.builder()
                .id(bookmark.getId())
                .user(UserEntity.from(bookmark.getUser()))
                .board(BoardEntity.from(bookmark.getBoard()))
                .build();
    }
}
