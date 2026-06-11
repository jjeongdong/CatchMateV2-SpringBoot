package com.back.catchmate.application.notice.service;

import com.back.catchmate.domain.common.page.DomainPage;
import com.back.catchmate.domain.common.page.DomainPageable;
import com.back.catchmate.domain.notice.model.Notice;
import com.back.catchmate.domain.notice.repository.NoticeRepository;
import com.back.catchmate.domain.user.model.User;
import com.back.catchmate.error.ErrorCode;
import com.back.catchmate.error.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeRepository noticeRepository;

    public Notice createNotice(User writer, String title, String content) {
        Notice notice = Notice.createNotice(writer, title, content);
        return noticeRepository.save(notice);
    }

    public Notice getNotice(Long noticeId) {
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
