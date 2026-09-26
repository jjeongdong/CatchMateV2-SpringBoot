package com.back.catchmate.notice.application;

import com.back.catchmate.global.response.OffsetPageResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.application.dto.result.NoticeResult;
import com.back.catchmate.notice.domain.Notice;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.user.application.UserQueryApi;
import com.back.catchmate.user.application.dto.api.UserInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeQueryService {
    private final NoticeRepository noticeRepository;
    private final UserQueryApi userQueryApi;

    @Transactional(readOnly = true)
    public NoticeDetailResult getNotice(Long noticeId) {
        Notice notice = noticeRepository.getById(noticeId);
        return NoticeDetailResult.of(
                notice, userQueryApi.getInfo(notice.getWriterId()).nickName());
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<NoticeResult> getNotices(int page, int size) {
        List<Notice> notices = noticeRepository.findAllLatest((long) page * size, size);
        long totalElements = noticeRepository.count();
        Map<Long, UserInfo> writerById = notices.isEmpty()
                ? Map.of()
                : userQueryApi.getInfos(
                        notices.stream().map(Notice::getWriterId).distinct().toList());
        // 작성자가 탈퇴해 조회되지 않으면 목록 전체를 실패시키지 않고 닉네임만 비운다 (옛 동작).
        List<NoticeResult> content = notices.stream()
                .map(notice -> NoticeResult.of(
                        notice,
                        writerById.containsKey(notice.getWriterId())
                                ? writerById.get(notice.getWriterId()).nickName()
                                : ""))
                .toList();
        return OffsetPageResult.of(content, page, size, totalElements);
    }
}
