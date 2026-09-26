package com.back.catchmate.inquiry.application;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryDetailResult;
import com.back.catchmate.inquiry.application.dto.result.AdminInquiryResult;
import com.back.catchmate.inquiry.application.dto.result.InquiryResult;
import com.back.catchmate.inquiry.domain.Inquiry;
import com.back.catchmate.inquiry.domain.InquiryRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryQueryService {
    private final InquiryRepository inquiryRepository;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public InquiryResult getMyInquiry(Long userId, Long inquiryId) {
        Inquiry inquiry = inquiryRepository.getById(inquiryId);
        inquiry.validateOwner(userId);
        return InquiryResult.of(inquiry, userQueryApi.getInfo(userId).nickName());
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<InquiryResult> getMyInquiries(Long userId, int page, int size) {
        List<Inquiry> inquiries = inquiryRepository.findAllLatestByUserId(userId, (long) page * size, size);
        long totalElements = inquiryRepository.countByUserId(userId);
        if (inquiries.isEmpty()) {
            return OffsetPageResult.of(List.of(), page, size, totalElements);
        }
        String nickname = userQueryApi.getInfo(userId).nickName();
        List<InquiryResult> content = inquiries.stream()
                .map(inquiry -> InquiryResult.of(inquiry, nickname))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<AdminInquiryResult> getInquiries(int page, int size) {
        List<Inquiry> inquiries = inquiryRepository.findAllLatest((long) page * size, size);
        long totalElements = inquiryRepository.count();
        Map<Long, UserInfo> writerById = inquiries.isEmpty()
                ? Map.of()
                : userQueryApi.getInfos(
                        inquiries.stream().map(Inquiry::getUserId).distinct().toList());
        List<AdminInquiryResult> content = inquiries.stream()
                .map(inquiry -> AdminInquiryResult.of(inquiry, writerById.get(inquiry.getUserId())))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }

    @Transactional(readOnly = true)
    public AdminInquiryDetailResult getInquiry(Long inquiryId) {
        Inquiry inquiry = inquiryRepository.getById(inquiryId);
        return AdminInquiryDetailResult.of(inquiry, userQueryApi.getInfo(inquiry.getUserId()));
    }
}
