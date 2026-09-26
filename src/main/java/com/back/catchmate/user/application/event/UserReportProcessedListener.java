package com.back.catchmate.user.application.event;

import com.back.catchmate.report.domain.event.ReportProcessedEvent;
import com.back.catchmate.user.application.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserReportProcessedListener {
    private final UserCommandService userCommandService;

    // 신고 처리와 유저 신고 표시가 한 트랜잭션으로 커밋돼야 한다(한쪽만 반영되면 재시도 수단이 없다).
    // 그래서 규칙 기본값(AFTER_COMMIT + REQUIRES_NEW) 대신 발행 트랜잭션에 참여하는 BEFORE_COMMIT 을 쓰고
    // 메서드에 @Transactional 을 붙이지 않는다. 표시가 실패하면 신고 처리도 롤백되고 예외가 호출자에게 간다.
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(ReportProcessedEvent event) {
        userCommandService.markUserAsReported(event.reportedUserId());
    }
}
