package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.response.InquiryCreateResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryClientCommandUseCase;
import com.back.catchmate.inquiry.application.port.out.persistence.InquiryRepository;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InquiryClientCommandService implements InquiryClientCommandUseCase {
    private final InquiryRepository inquiryRepository;

    @Override
    public InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command) {
        Inquiry inquiry = Inquiry.createInquiry(userId, command.type(), command.content());
        Inquiry saved = inquiryRepository.save(inquiry);
        return toCreateResponse(saved);
    }

    private InquiryCreateResponse toCreateResponse(Inquiry inquiry) {
        return new InquiryCreateResponse(inquiry.getId(), inquiry.getCreatedAt());
    }
}
