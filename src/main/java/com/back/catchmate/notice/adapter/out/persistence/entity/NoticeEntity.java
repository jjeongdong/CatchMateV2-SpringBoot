package com.back.catchmate.notice.adapter.out.persistence.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.notice.domain.model.Notice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "notices")
public class NoticeEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long writerId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public static NoticeEntity from(Notice notice) {
        return NoticeEntity.builder()
                .id(notice.getId())
                .writerId(notice.getWriterId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .build();
    }

    public Notice toDomain() {
        return Notice.builder()
                .id(this.id)
                .writerId(this.writerId)
                .title(this.title)
                .content(this.content)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
