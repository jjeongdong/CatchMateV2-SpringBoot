package com.back.catchmate.orchestration.notice;

import com.back.catchmate.application.notice.service.NoticeService;
import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.orchestration.common.PagedResponse;
import com.back.catchmate.orchestration.notice.dto.response.NoticeDetailResponse;
import com.back.catchmate.orchestration.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeOrchestrator {
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
