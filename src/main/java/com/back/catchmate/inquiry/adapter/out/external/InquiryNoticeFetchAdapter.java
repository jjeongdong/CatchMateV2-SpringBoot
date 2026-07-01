package com.back.catchmate.inquiry.adapter.out.external;

import com.back.catchmate.inquiry.application.port.out.dto.AssistNoticeInfo;
import com.back.catchmate.inquiry.application.port.out.external.NoticeFetchPort;
import com.back.catchmate.notice.application.port.in.NoticeInternalQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * NoticeFetchPort 구현 — notice 읽기 정문({@link NoticeInternalQueryUseCase})을 호출해
 * 공지를 가져오고, notice 타입을 inquiry 소유 DTO({@link AssistNoticeInfo})로 격리한다.
 */
@Component
@RequiredArgsConstructor
public class InquiryNoticeFetchAdapter implements NoticeFetchPort {
    private final NoticeInternalQueryUseCase noticeInternalQueryUseCase;

    @Override
    public List<AssistNoticeInfo> fetchAll() {
        return noticeInternalQueryUseCase.getAllNotices().stream()
                .map(notice -> new AssistNoticeInfo(notice.noticeId(), notice.title(), notice.content()))
                .toList();
    }
}
