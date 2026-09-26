package com.back.catchmate.notice.application;

import com.back.catchmate.notice.application.dto.command.NoticeCreateCommand;
import com.back.catchmate.notice.application.dto.command.NoticeUpdateCommand;
import com.back.catchmate.notice.application.dto.result.NoticeCreateResult;
import com.back.catchmate.notice.application.dto.result.NoticeDetailResult;
import com.back.catchmate.notice.domain.Notice;
import com.back.catchmate.notice.domain.NoticeRepository;
import com.back.catchmate.notice.domain.event.NoticeCreatedEvent;
import com.back.catchmate.user.application.UserQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoticeCommandService {
    private final NoticeRepository noticeRepository;
    private final UserQueryApi userQueryApi;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public NoticeCreateResult createNotice(Long writerId, NoticeCreateCommand command) {
        Notice notice = noticeRepository.save(Notice.create(writerId, command.title(), command.content()));
        eventPublisher.publishEvent(new NoticeCreatedEvent(notice.getId(), notice.getTitle()));
        return NoticeCreateResult.from(notice);
    }

    @Transactional
    public NoticeDetailResult updateNotice(Long noticeId, NoticeUpdateCommand command) {
        Notice notice = noticeRepository.getById(noticeId);
        notice.revise(command.title(), command.content());
        return NoticeDetailResult.of(
                notice, userQueryApi.getInfo(notice.getWriterId()).nickName());
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.delete(noticeRepository.getById(noticeId));
    }
}
