package com.back.catchmate.notice.application;

import com.back.catchmate.notice.application.dto.api.NoticeInfo;
import com.back.catchmate.notice.domain.NoticeRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeQueryApi {
    private final NoticeRepository noticeRepository;

    /**
     * 최신 공지를 최대 limit 건 조회한다 (AI 문의 답변 코퍼스 색인용).
     *
     * @param limit 최대 건수
     * @return 최신 작성순 공지 정보 목록
     */
    @Transactional(readOnly = true)
    public List<NoticeInfo> getLatestInfos(int limit) {
        return noticeRepository.findAllLatest(0, limit).stream()
                .map(NoticeInfo::from)
                .toList();
    }
}
