package com.back.catchmate.notice.application.service;

import com.back.catchmate.notice.application.dto.response.NoticeInternalResponse;
import com.back.catchmate.notice.application.port.in.NoticeAdminQueryUseCase;
import com.back.catchmate.notice.domain.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeInternalQueryService implements NoticeAdminQueryUseCase {
    private final NoticeReader noticeReader;

    @Override
    public NoticeInternalResponse getNotice(Long noticeId) {
        return toInternalResponse(noticeReader.getNotice(noticeId));
    }

    @Override
    public Page<NoticeInternalResponse> getNoticeList(Pageable pageable) {
        return noticeReader.getNoticeList(pageable).map(this::toInternalResponse);
    }

    private NoticeInternalResponse toInternalResponse(Notice notice) {
        return new NoticeInternalResponse(
                notice.getId(),
                notice.getWriterId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCreatedAt()
        );
    }
}
