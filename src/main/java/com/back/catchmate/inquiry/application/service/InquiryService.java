package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.inquiry.application.port.out.UserFetchPort;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.orchestration.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.back.catchmate.inquiry.application.dto.command.InquiryCreateCommand;
import com.back.catchmate.inquiry.application.dto.response.InquiryCreateResponse;
import com.back.catchmate.inquiry.application.dto.response.InquiryDetailResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryUseCase;
import com.back.catchmate.inquiry.application.port.out.InquiryRepository;
import com.back.catchmate.inquiry.domain.enums.InquiryType;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import com.back.catchmate.inquiry.domain.model.InquiryStatus;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryService implements InquiryUseCase {

    private final InquiryRepository inquiryRepository;

    private final UserFetchPort userFetchPort;

    @Transactional
    public InquiryCreateResponse createInquiry(Long userId, InquiryCreateCommand command) {
        User user = userFetchPort.getUser(userId);

        Inquiry inquiry = registerInquiry(
                user,
                command.getType(),
                command.getContent()
        );

        return InquiryCreateResponse.of(inquiry.getId());
    }

    public InquiryDetailResponse getInquiry(Long inquiryId) {
        Inquiry inquiry = getInquiryEntity(inquiryId);
        return InquiryDetailResponse.from(inquiry);
    }

    public PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inquiry> inquiryPage = getInquiryListByUser(userId, pageable);

        List<InquiryDetailResponse> responses = inquiryPage.getContent().stream()
                .map(InquiryDetailResponse::from)
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    public Inquiry registerInquiry(User user, InquiryType type, String content) {
        Inquiry inquiry = Inquiry.createInquiry(user, type, content);
        return inquiryRepository.save(inquiry);
    }

    public Inquiry getInquiryEntity(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new BaseException(ErrorCode.INQUIRY_NOT_FOUND));
    }

    public Page<Inquiry> getInquiryList(Pageable pageable) {
        return inquiryRepository.findAll(pageable);
    }

    public Page<Inquiry> getInquiryListByUser(Long userId, Pageable pageable) {
        return inquiryRepository.findAllByUserId(userId, pageable);
    }

    public long getTotalInquiryCount() {
        return inquiryRepository.count();
    }

    public long getWaitingInquiryCount() {
        return inquiryRepository.countByStatus(InquiryStatus.WAITING);
    }

    public Inquiry updateInquiry(Inquiry inquiry) {
        return inquiryRepository.save(inquiry);
    }
}
