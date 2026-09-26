package com.back.catchmate.inquiry.application;

import com.back.catchmate.inquiry.application.dto.command.InquiryAnswerCreateCommand;
import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.result.InquiryAnswerCreateResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryCreateResult;
import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.inquiry.domain.event.InquiryAnswerRegisteredEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryCommandService {
    private final InquiryRepository inquiryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InquiryCreateResult createInquiry(Long userId, InquiryCreateCommand command) {
        Inquiry inquiry = inquiryRepository.save(Inquiry.create(userId, command.type(), command.content()));
        return InquiryCreateResult.from(inquiry);
    }

    @Transactional
    public InquiryAnswerCreateResult createInquiryAnswer(Long inquiryId, InquiryAnswerCreateCommand command) {
        Inquiry inquiry = inquiryRepository.getById(inquiryId);
        inquiry.registerAnswer(command.content());
        eventPublisher.publishEvent(new InquiryAnswerRegisteredEvent(inquiry.getId(), inquiry.getUserId()));
        // 엔티티의 수정 시각은 flush 전이라 아직 갱신되지 않아 응답 시각을 직접 채운다.
        return new InquiryAnswerCreateResult(inquiry.getId(), inquiry.getUserId(), LocalDateTime.now());
    }
}
