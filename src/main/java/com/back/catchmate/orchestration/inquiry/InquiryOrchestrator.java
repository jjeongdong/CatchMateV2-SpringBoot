package com.back.catchmate.orchestration.inquiry;

import com.back.catchmate.application.inquiry.service.InquiryService;
import com.back.catchmate.application.user.service.UserService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.inquiry.model.Inquiry;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.inquiry.dto.command.InquiryCreateCommand;
import com.back.catchmate.orchestration.inquiry.dto.response.InquiryCreateResponse;
import com.back.catchmate.orchestration.inquiry.dto.response.InquiryDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryOrchestrator {
    private final InquiryService inquiryService;
    private final UserService userService;

    @Transactional
    public InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command) {
        User user = userService.getUser(userId);

        Inquiry inquiry = inquiryService.registerInquiry(
                user,
                command.getType(),
                command.getContent()
        );

        return InquiryCreateResponse.of(inquiry.getId());
    }

    public InquiryDetailResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryService.getInquiry(inquiryId);
        return InquiryDetailResponse.from(inquiry);
    }

    public PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size) {
        DomainPageable pageable = DomainPageable.of(page, size);
        DomainPage<Inquiry> inquiryPage = inquiryService.getInquiryListByUser(userId, pageable);

        List<InquiryDetailResponse> responses = inquiryPage.getContent().stream()
                .map(InquiryDetailResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }
}
