package com.back.catchmate.notice.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.orchestration.PagedResponse;
import com.back.catchmate.common.page.DomainPage;
import com.back.catchmate.common.page.DomainPageable;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.application.port.in.NoticeUseCase;
import com.back.catchmate.notice.application.port.out.NoticeRepository;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.user.domain.model.User;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeService implements NoticeUseCase {

    private final NoticeRepository noticeRepository;

    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = getNoticeEntity(noticeId);
        return NoticeDetailResponse.from(notice);
    }

    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        DomainPageable domainPageable = new DomainPageable(page, size);
        DomainPage<Notice> noticePage = getNoticeList(domainPageable);

        List<NoticeResponse> responses = noticePage.getContent().stream()
                .map(NoticeResponse::from)
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    public Notice createNotice(User writer, String title, String content) {
        Notice notice = Notice.createNotice(writer, title, content);
        return noticeRepository.save(notice);
    }

    public Notice getNoticeEntity(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTICE_NOT_FOUND));
    }

    public DomainPage<Notice> getNoticeList(DomainPageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    public Notice updateNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    public void deleteNotice(Notice notice) {
        noticeRepository.delete(notice);
    }
}
