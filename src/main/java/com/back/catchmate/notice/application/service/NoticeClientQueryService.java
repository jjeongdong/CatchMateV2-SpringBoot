package com.back.catchmate.notice.application.service;

import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.application.port.out.dto.NoticeUserInfo;
import com.back.catchmate.notice.application.port.in.NoticeClientQueryUseCase;
import com.back.catchmate.notice.application.port.out.external.UserFetchPort;
import com.back.catchmate.notice.domain.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class NoticeClientQueryService implements NoticeClientQueryUseCase {
    private final NoticeReader noticeReader;
    private final UserFetchPort userFetchPort;

    @Override
    public NoticeDetailResponse getNoticeDetail(Long noticeId) {
        Notice notice = noticeReader.getNotice(noticeId);
        NoticeUserInfo writer = userFetchPort.getUser(notice.getWriterId());
        return toDetailResponse(notice, writer.nickname());
    }

    @Override
    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> noticePage = noticeReader.getNoticeList(pageable);

        Map<Long, String> writerNicknameById = fetchWriterNicknames(noticePage.getContent());

        List<NoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> toNoticeResponse(n, writerNicknameById.getOrDefault(n.getWriterId(), "")))
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    private NoticeDetailResponse toDetailResponse(Notice notice, String writerNickname) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                writerNickname,
                notice.getCreatedAt()
        );
    }

    private NoticeResponse toNoticeResponse(Notice notice, String writerNickname) {
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                writerNickname,
                notice.getCreatedAt()
        );
    }

    private Map<Long, String> fetchWriterNicknames(List<Notice> notices) {
        List<Long> writerIds = notices.stream().map(Notice::getWriterId).distinct().toList();
        if (writerIds.isEmpty()) return Map.of();
        return userFetchPort.getUsers(writerIds).stream()
                .collect(Collectors.toMap(NoticeUserInfo::userId, NoticeUserInfo::nickname));
    }
}
