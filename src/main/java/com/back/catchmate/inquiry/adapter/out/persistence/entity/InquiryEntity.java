package com.back.catchmate.inquiry.adapter.out.persistence.entity;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import com.back.catchmate.inquiry.domain.model.InquiryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "inquiries")
public class InquiryEntity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String answer;

    @Enumerated(EnumType.STRING)
    private InquiryType type;

    @Enumerated(EnumType.STRING)
    private InquiryStatus status;

    public static InquiryEntity from(Inquiry inquiry) {
        return InquiryEntity.builder()
                .id(inquiry.getId())
                .userId(inquiry.getUserId())
                .content(inquiry.getContent())
                .answer(inquiry.getAnswer())
                .status(inquiry.getStatus())
                .type(inquiry.getType())
                .build();
    }

    public Inquiry toDomain() {
        return Inquiry.builder()
                .id(this.id)
                .userId(this.userId)
                .type(this.type)
                .content(this.content)
                .answer(this.answer)
                .status(this.status)
                .createdAt(this.getCreatedAt())
                .build();
    }
}
