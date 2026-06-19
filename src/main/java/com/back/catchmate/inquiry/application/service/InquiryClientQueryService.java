package com.back.catchmate.inquiry.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.inquiry.application.port.out.dto.InquiryUserInfo;
import com.back.catchmate.inquiry.application.dto.response.InquiryDetailResponse;
import com.back.catchmate.inquiry.application.port.in.InquiryClientQueryUseCase;
import com.back.catchmate.inquiry.application.port.out.external.UserFetchPort;
import com.back.catchmate.inquiry.domain.model.Inquiry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryClientQueryService implements InquiryClientQueryUseCase {
    private final InquiryReader inquiryReader;
    private final UserFetchPort userFetchPort;

    @Override
    public InquiryDetailResponse getInquiryDetail(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryReader.getInquiry(inquiryId);
        if (!inquiry.getUserId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN_ACCESS);
        }
        InquiryUserInfo inquirer = userFetchPort.getUser(inquiry.getUserId());
        return toDetailResponse(inquiry, inquirer.nickname());
    }

    @Override
    public PagedResponse<InquiryDetailResponse> getInquiryListByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inquiry> inquiryPage = inquiryReader.getInquiryListByUser(userId, pageable);

        if (inquiryPage.isEmpty()) {
            return new PagedResponse<>(inquiryPage, List.of());
        }

        String nickname = userFetchPort.getUser(userId).nickname();

        List<InquiryDetailResponse> responses = inquiryPage.getContent().stream()
                .map(i -> toDetailResponse(i, nickname))
                .toList();

        return new PagedResponse<>(inquiryPage, responses);
    }

    private InquiryDetailResponse toDetailResponse(Inquiry inquiry, String nickname) {
        return new InquiryDetailResponse(
                inquiry.getId(),
                nickname,
                inquiry.getType().getDescription(),
                inquiry.getContent(),
                inquiry.getAnswer(),
                inquiry.getStatus().getDescription(),
                inquiry.getCreatedAt()
        );
    }
}
