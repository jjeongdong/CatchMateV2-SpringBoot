package com.back.catchmate.notification.application.event;

import static com.back.catchmate.notification.fixture.NotificationFixture.user;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.chat.application.ChatMessageWriter;
import com.back.catchmate.chat.application.ChatQueryApi;
import com.back.catchmate.chat.application.dto.api.ChatRecipientInfo;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.infrastructure.ChatMessageRepositoryImpl;
import com.back.catchmate.chat.infrastructure.ChatRoomJpaRepository;
import com.back.catchmate.chat.infrastructure.ChatRoomRepositoryImpl;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import com.back.catchmate.notification.application.ChatNotificationService;
import com.back.catchmate.notification.application.OutboxDispatcher;
import com.back.catchmate.notification.application.OutboxWriter;
import com.back.catchmate.notification.domain.RealtimeNotificationPublisher;
import com.back.catchmate.user.application.UserQueryApi;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// ChatNotificationPreparedEvent 는 메시지 트랜잭션의 BEFORE_COMMIT 리스너 안에서 발행된다. 그 이벤트의 AFTER_COMMIT
// 리스너가 실제로 커밋 뒤에 불리는지(롤백되면 안 불리는지)는 Spring 트랜잭션 동기화 동작에 기대므로 실제 DB 트랜잭션으로 확인한다.
// 커밋이 일어나야 하므로 테스트 트랜잭션을 끈다. 이 슬라이스엔 @EnableAsync 가 없어 발송 리스너가 동기로 돈다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    QuerydslConfig.class,
    JpaAuditingConfig.class,
    ChatMessageWriter.class,
    ChatRoomRepositoryImpl.class,
    ChatMessageRepositoryImpl.class,
    ChatNotificationService.class,
    NotificationChatMessageSentListener.class,
    NotificationChatNotificationPreparedListener.class
})
@Testcontainers(disabledWithoutDocker = true)
class NotificationChatNotificationPreparedListenerIntegrationTest {

    private static final Long SENDER_ID = 1L;
    private static final Long RECIPIENT_ID = 2L;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @MockitoBean
    private UserQueryApi userQueryApi;

    @MockitoBean
    private ChatQueryApi chatQueryApi;

    @MockitoBean
    private OutboxWriter outboxWriter;

    @MockitoBean
    private OutboxDispatcher outboxDispatcher;

    @MockitoBean
    private RealtimeNotificationPublisher realtimeNotificationPublisher;

    @Autowired
    private ChatMessageWriter chatMessageWriter;

    @Autowired
    private ChatRoomJpaRepository chatRoomJpaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("저장 단계(BEFORE_COMMIT)에서 발행한 발송 이벤트는 메시지가 커밋된 뒤 발송된다")
    void dispatchesAfterCommit() {
        // given
        Long chatRoomId = givenRoomWithUnfocusedRecipient(100L);

        // when
        chatMessageWriter.writeText(chatRoomId, SENDER_ID, "안녕", 1L);

        // then
        then(realtimeNotificationPublisher).should().publishAll(eq(List.of(RECIPIENT_ID)), anyMap());
        then(outboxDispatcher).should().sendPendingOutboxesImmediately(List.of(RECIPIENT_ID));
    }

    @Test
    @DisplayName("메시지 트랜잭션이 롤백되면 발송하지 않는다")
    void doesNotDispatchOnRollback() {
        // given — 메시지 저장이 바깥 트랜잭션에 참여한 뒤 바깥이 롤백된다
        Long chatRoomId = givenRoomWithUnfocusedRecipient(101L);
        TransactionTemplate outer = new TransactionTemplate(transactionManager);

        // when
        outer.executeWithoutResult(status -> {
            chatMessageWriter.writeText(chatRoomId, SENDER_ID, "안녕", 1L);
            status.setRollbackOnly();
        });

        // then
        then(realtimeNotificationPublisher).should(never()).publishAll(eq(List.of(RECIPIENT_ID)), anyMap());
        then(outboxDispatcher).should(never()).sendPendingOutboxesImmediately(List.of(RECIPIENT_ID));
    }

    private Long givenRoomWithUnfocusedRecipient(Long boardId) {
        Long chatRoomId = chatRoomJpaRepository.save(ChatRoom.create(boardId)).getId();
        given(chatQueryApi.getRecipients(chatRoomId, SENDER_ID))
                .willReturn(List.of(new ChatRecipientInfo(RECIPIENT_ID, true)));
        given(userQueryApi.getInfo(SENDER_ID)).willReturn(user(SENDER_ID, "철수", null, true, true, true));
        given(userQueryApi.getInfos(List.of(RECIPIENT_ID)))
                .willReturn(Map.of(RECIPIENT_ID, user(RECIPIENT_ID, "영희", "token", true, true, true)));
        given(chatQueryApi.getFocusRooms(List.of(RECIPIENT_ID))).willReturn(Map.of());
        return chatRoomId;
    }
}
