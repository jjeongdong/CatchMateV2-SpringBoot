package com.back.catchmate.user.application.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.back.catchmate.report.domain.event.ReportProcessedEvent;
import com.back.catchmate.user.application.UserCommandService;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class UserReportProcessedListenerTest {

    @Mock
    private UserCommandService userCommandService;

    @InjectMocks
    private UserReportProcessedListener listener;

    @Test
    @DisplayName("신고 처리 이벤트를 받으면 피신고자를 신고됨으로 표시한다")
    void marksReportedUser() {
        // when
        listener.handle(new ReportProcessedEvent(5L, 2L));

        // then
        then(userCommandService).should().markUserAsReported(2L);
    }

    @Test
    @DisplayName("신고 처리와 한 트랜잭션으로 커밋되도록 BEFORE_COMMIT 에서 발행 트랜잭션에 참여한다")
    void joinsPublisherTransactionBeforeCommit() throws Exception {
        // when
        Method handle = UserReportProcessedListener.class.getMethod("handle", ReportProcessedEvent.class);

        // then
        assertThat(handle.getAnnotation(TransactionalEventListener.class).phase())
                .isEqualTo(TransactionPhase.BEFORE_COMMIT);
        assertThat(handle.isAnnotationPresent(Transactional.class)).isFalse();
    }
}
