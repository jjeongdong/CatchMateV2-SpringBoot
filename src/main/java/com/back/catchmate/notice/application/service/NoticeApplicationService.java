package com.back.catchmate.notice.application.service;


import com.back.catchmate.notice.application.port.in.NoticeUseCase;
import com.back.catchmate.notice.application.service.NoticeService;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeApplicationService implements NoticeUseCase {
    private final NoticeService noticeService;

    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = noticeService.getNotice(noticeId);
        return NoticeDetailResponse.from(notice);
    }

    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Notice> noticePage = noticeService.getNoticeList(domainPageable);

        List<NoticeResponse> responses = noticePage.getContent().stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }
}
