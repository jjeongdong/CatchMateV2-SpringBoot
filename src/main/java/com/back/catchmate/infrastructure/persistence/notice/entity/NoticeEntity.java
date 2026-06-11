package com.back.catchmate.infrastructure.persistence.notice.entity;

import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.infrastructure.global.BaseTimeEntity;
import com.back.catchmate.infrastructure.persistence.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "notices")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class NoticeEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity writer;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public static NoticeEntity from(Notice notice) {
        return NoticeEntity.builder()
                .id(notice.getId())
                .writer(UserEntity.from(notice.getWriter()))
                .title(notice.getTitle())
                .content(notice.getContent())
                .build();
    }

    public Notice toModel() {
        return Notice.builder()
                .id(this.id)
                .writer(this.writer.toModel())
                .title(this.title)
                .content(this.content)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
