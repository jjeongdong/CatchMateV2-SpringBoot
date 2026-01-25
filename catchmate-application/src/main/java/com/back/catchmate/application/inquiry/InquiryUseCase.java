package com.back.catchmate.application.inquiry;

import com.back.catchmate.application.inquiry.dto.command.InquiryCreateCommand;
import com.back.catchmate.application.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.application.inquiry.dto.response.InquiryDetailResponse;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.inquiry.service.InquiryService;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.domain.user.service.UserService;
import error.ErrorCode;
import error.exception.BaseException;
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
    public InquiryCreateResponse registerInquiry(Long userId, InquiryCreateCommand command) {
        User user = userService.getUserById(userId);

        Inquiry inquiry = Inquiry.createInquiry(
                user,
                command.getType(),
                command.getTitle(),
                command.getContent()
        );

        return InquiryCreateResponse.of(inquiry.getId());
    }

    public InquiryDetailResponse getInquiryDetail(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);

        // 본인 글인지 확인
        if (!inquiry.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return InquiryDetailResponse.from(inquiry);
    }
}
