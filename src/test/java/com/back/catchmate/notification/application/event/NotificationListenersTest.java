package com.back.catchmate.notification.application.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.chat.domain.event.ChatMessageSentEvent;
import com.back.catchmate.enroll.domain.event.EnrollAcceptedEvent;
import com.back.catchmate.enroll.domain.event.EnrollCancelledEvent;
import com.back.catchmate.enroll.domain.event.EnrollRejectedEvent;
import com.back.catchmate.enroll.domain.event.EnrollRequestedEvent;
import com.back.catchmate.inquiry.domain.event.InquiryAnswerRegisteredEvent;
import com.back.catchmate.notice.domain.event.NoticeCreatedEvent;
import com.back.catchmate.notification.domain.event.ChatNotificationPreparedEvent;
import java.lang.reflect.Method;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class NotificationListenersTest {

    private static final Map<Class<?>, Class<?>> SAVE_LISTENERS = Map.of(
            NotificationEnrollRequestedListener.class, EnrollRequestedEvent.class,
            NotificationEnrollAcceptedListener.class, EnrollAcceptedEvent.class,
            NotificationEnrollRejectedListener.class, EnrollRejectedEvent.class,
            NotificationEnrollCancelledListener.class, EnrollCancelledEvent.class,
            NotificationChatMessageSentListener.class, ChatMessageSentEvent.class,
            NotificationInquiryAnswerRegisteredListener.class, InquiryAnswerRegisteredEvent.class,
            NotificationNoticeCreatedListener.class, NoticeCreatedEvent.class);

    // 채팅은 저장 단계가 발송 대상을 정해 ChatNotificationPreparedEvent 로 넘기므로 발송 리스너가 따로 있다.
    private static final Map<Class<?>, Class<?>> DISPATCH_LISTENERS = Map.of(
            NotificationEnrollRequestedListener.class, EnrollRequestedEvent.class,
            NotificationEnrollAcceptedListener.class, EnrollAcceptedEvent.class,
            NotificationEnrollRejectedListener.class, EnrollRejectedEvent.class,
            NotificationEnrollCancelledListener.class, EnrollCancelledEvent.class,
            NotificationChatNotificationPreparedListener.class, ChatNotificationPreparedEvent.class,
            NotificationInquiryAnswerRegisteredListener.class, InquiryAnswerRegisteredEvent.class,
            NotificationNoticeCreatedListener.class, NoticeCreatedEvent.class);

    @Test
    @DisplayName("저장은 원래 작업과 같은 트랜잭션(BEFORE_COMMIT)에서 한다 — 커밋 후 저장 전 장애로 알림이 사라지지 않게")
    void savesBeforeCommit() throws Exception {
        for (Map.Entry<Class<?>, Class<?>> entry : SAVE_LISTENERS.entrySet()) {
            Method handle = entry.getKey().getMethod("handle", entry.getValue());

            assertThat(handle.getAnnotation(TransactionalEventListener.class).phase())
                    .as(entry.getKey().getSimpleName())
                    .isEqualTo(TransactionPhase.BEFORE_COMMIT);
            assertThat(handle.isAnnotationPresent(Async.class)).isFalse();
        }
    }

    @Test
    @DisplayName("발송은 커밋 후 알림 전용 executor 에서 비동기로, 트랜잭션 없이 한다")
    void dispatchesAfterCommitAsync() throws Exception {
        for (Map.Entry<Class<?>, Class<?>> entry : DISPATCH_LISTENERS.entrySet()) {
            Method dispatch = entry.getKey().getMethod("dispatch", entry.getValue());

            assertThat(dispatch.getAnnotation(TransactionalEventListener.class).phase())
                    .as(entry.getKey().getSimpleName())
                    .isEqualTo(TransactionPhase.AFTER_COMMIT);
            assertThat(dispatch.getAnnotation(Async.class).value()).isEqualTo("notificationDispatchExecutor");
            assertThat(dispatch.isAnnotationPresent(Transactional.class)).isFalse();
        }
    }
}
