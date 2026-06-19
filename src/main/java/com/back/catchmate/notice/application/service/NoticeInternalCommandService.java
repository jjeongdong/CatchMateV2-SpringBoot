package com.back.catchmate.notice.application.service;

import com.back.catchmate.notice.application.dto.command.NoticeInternalCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeInternalUpdateCommand;
import com.back.catchmate.notice.application.dto.response.NoticeInternalCreateResponse;
import com.back.catchmate.notice.application.port.in.NoticeInternalCommandUseCase;
import com.back.catchmate.notice.application.port.out.persistence.NoticeRepository;
import com.back.catchmate.notice.domain.model.Notice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class NoticeInternalCommandService implements NoticeInternalCommandUseCase {
    private final NoticeRepository noticeRepository;
    private final NoticeReader noticeReader;

    @Override
    public NoticeInternalCreateResponse createNotice(NoticeInternalCreateCommand command) {
        Notice notice = Notice.createNotice(command.writerId(), command.title(), command.content());
        Notice savedNotice = noticeRepository.save(notice);
        return toCreateResponse(savedNotice);
    }

    @Override
    public void updateNotice(NoticeInternalUpdateCommand command) {
        Notice notice = noticeReader.getNotice(command.noticeId());
        notice.updateNotice(command.title(), command.content());
        noticeRepository.save(notice);
    }

    @Override
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeReader.getNotice(noticeId);
        noticeRepository.delete(notice);
    }

    private NoticeInternalCreateResponse toCreateResponse(Notice notice) {
        return new NoticeInternalCreateResponse(
                notice.getId(),
                notice.getCreatedAt()
        );
    }
}
