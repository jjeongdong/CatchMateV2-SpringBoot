package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.dto.command.InquiryInternalRegisterAnswerCommand;
import com.back.catchmate.inquiry.application.port.in.InquiryInternalCommandUseCase;
import com.back.catchmate.inquiry.application.port.out.persistence.InquiryRepository;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InquiryInternalCommandService implements InquiryInternalCommandUseCase {
    private final InquiryRepository inquiryRepository;
    private final InquiryReader inquiryReader;

    @Override
    public void registerAnswer(InquiryInternalRegisterAnswerCommand command) {
        Inquiry inquiry = inquiryReader.getInquiry(command.inquiryId());
        inquiry.registerAnswer(command.content());
        inquiryRepository.save(inquiry);
    }
}
