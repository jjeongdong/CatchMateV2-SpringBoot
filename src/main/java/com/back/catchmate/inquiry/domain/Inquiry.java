package com.back.catchmate.inquiry.domain;

import com.back.catchmate.global.persistence.BaseTimeEntity;
import com.back.catchmate.inquiry.domain.exception.InquiryAlreadyAnsweredException;
import com.back.catchmate.inquiry.domain.exception.InquiryNotOwnerException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inquiries")
public class Inquiry extends BaseTimeEntity {
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

    private Inquiry(Long userId, InquiryType type, String content) {
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.status = InquiryStatus.WAITING;
    }

    public static Inquiry create(Long userId, InquiryType type, String content) {
        return new Inquiry(userId, type, content);
    }

    public void registerAnswer(String answer) {
        if (this.status == InquiryStatus.ANSWERED) {
            throw new InquiryAlreadyAnsweredException();
        }
        this.answer = answer;
        this.status = InquiryStatus.ANSWERED;
    }

    public void validateOwner(Long userId) {
        if (!this.userId.equals(userId)) {
            throw new InquiryNotOwnerException();
        }
    }
}
