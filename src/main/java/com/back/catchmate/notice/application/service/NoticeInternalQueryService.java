package com.back.catchmate.notice.application.service;

import com.back.catchmate.notice.application.dto.response.NoticeInternalResponse;
import com.back.catchmate.notice.application.port.in.NoticeAdminQueryUseCase;
import com.back.catchmate.notice.application.port.in.NoticeInternalQueryUseCase;
import com.back.catchmate.notice.domain.model.Notice;
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
public class NoticeInternalQueryService implements NoticeAdminQueryUseCase, NoticeInternalQueryUseCase {
    // 코퍼스 색인용 일괄 조회 상한 (공지는 소규모라 단일 페이지로 충분)
    private static final int MAX_CORPUS_FETCH = 1000;

    private final NoticeReader noticeReader;

    @Override
    public NoticeInternalResponse getNotice(Long noticeId) {
        return toInternalResponse(noticeReader.getNotice(noticeId));
    }

    @Override
    public Page<NoticeInternalResponse> getNoticeList(Pageable pageable) {
        return noticeReader.getNoticeList(pageable).map(this::toInternalResponse);
    }

    @Override
    public List<NoticeInternalResponse> getAllNotices() {
        return noticeReader.getNoticeList(PageRequest.of(0, MAX_CORPUS_FETCH))
                .map(this::toInternalResponse)
                .getContent();
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
