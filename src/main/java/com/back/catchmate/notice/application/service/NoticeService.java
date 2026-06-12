package com.back.catchmate.notice.application.service;

import com.back.catchmate.common.error.ErrorCode;
import com.back.catchmate.common.error.exception.BaseException;
import com.back.catchmate.common.response.PagedResponse;
import com.back.catchmate.notice.application.dto.response.NoticeDetailResponse;
import com.back.catchmate.notice.application.dto.response.NoticeResponse;
import com.back.catchmate.notice.application.port.in.NoticeUseCase;
import com.back.catchmate.notice.application.port.out.NoticeRepository;
import com.back.catchmate.notice.application.port.out.UserFetchPort;
import com.back.catchmate.notice.domain.model.Notice;
import com.back.catchmate.user.domain.model.User;
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
public class NoticeService implements NoticeUseCase {

    private final NoticeRepository noticeRepository;

    private final UserFetchPort userFetchPort;

    public NoticeDetailResponse getNotice(Long noticeId) {
        Notice notice = getNoticeEntity(noticeId);
        User writer = userFetchPort.getUser(notice.getWriterId());
        return NoticeDetailResponse.from(notice, writer.getNickName());
    }

    public PagedResponse<NoticeResponse> getNoticeList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Notice> noticePage = getNoticeList(pageable);

        Map<Long, String> writerNicknameById = fetchWriterNicknames(noticePage.getContent());

        List<NoticeResponse> responses = noticePage.getContent().stream()
                .map(n -> NoticeResponse.from(n, writerNicknameById.getOrDefault(n.getWriterId(), "")))
                .collect(Collectors.toList());

        return new PagedResponse<>(noticePage, responses);
    }

    public Notice createNotice(Long writerId, String title, String content) {
        Notice notice = Notice.createNotice(writerId, title, content);
        return noticeRepository.save(notice);
    }

    public Notice getNoticeEntity(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BaseException(ErrorCode.NOTICE_NOT_FOUND));
    }

    public Page<Notice> getNoticeList(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    public Notice updateNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    public void deleteNotice(Notice notice) {
        noticeRepository.delete(notice);
    }

    private Map<Long, String> fetchWriterNicknames(List<Notice> notices) {
        List<Long> writerIds = notices.stream().map(Notice::getWriterId).distinct().toList();
        if (writerIds.isEmpty()) return Map.of();
        return userFetchPort.getUsers(writerIds).stream()
                .collect(Collectors.toMap(User::getId, User::getNickName));
    }
}
