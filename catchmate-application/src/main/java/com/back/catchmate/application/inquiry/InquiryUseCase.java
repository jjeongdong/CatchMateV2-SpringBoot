package com.back.catchmate.application.inquiry;

import com.back.catchmate.application.inquiry.dto.command.InquiryCreateCommand;
import com.back.catchmate.application.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.application.inquiry.dto.response.InquiryDetailResponse;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.service.InquiryService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryUseCase {
    private final InquiryService inquiryService;
    private final UserService userService;

    @Transactional
    public InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command) {
        User user = userService.getUser(userId);

        Inquiry inquiry = Inquiry.createInquiry(
                user,
                command.getType(),
                command.getTitle(),
                command.getContent()
        );

        Inquiry savedInquiry = inquiryService.createInquiry(inquiry);
        return InquiryCreateResponse.of(savedInquiry.getId());
    }

    public InquiryDetailResponse getInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);
        return InquiryDetailResponse.from(inquiry);
    }
}
