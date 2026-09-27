package com.back.catchmate.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.back.catchmate.notification.domain.NotificationOutbox;
import com.back.catchmate.notification.domain.NotificationOutboxRepository;
import com.back.catchmate.notification.domain.OutboxRecipient;
import com.back.catchmate.notification.domain.OutboxStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxWriterTest {

    @Mock
    private NotificationOutboxRepository notificationOutboxRepository;

    private OutboxWriter outboxWriter;

    @BeforeEach
    void setUp() {
        outboxWriter = new OutboxWriter(notificationOutboxRepository, new ObjectMapper());
    }

    @Test
    @DisplayName("단건은 payload 를 JSON 으로 담아 PENDING 으로 저장한다")
    void write() {
        outboxWriter.write(1L, "token", "제목", "본문", Map.of("type", "INQUIRY"));

        ArgumentCaptor<NotificationOutbox> captor = ArgumentCaptor.forClass(NotificationOutbox.class);
        then(notificationOutboxRepository).should().save(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"type\":\"INQUIRY\"}");
        assertThat(captor.getValue().getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("여러 수신자는 한 번의 배치로 적재하고, 수신자가 없으면 아무것도 하지 않는다")
    @SuppressWarnings("unchecked")
    void writeAll() {
        outboxWriter.writeAll(
                List.of(new OutboxRecipient(1L, "t1"), new OutboxRecipient(2L, "t2")), "제목", "본문", Map.of());
        outboxWriter.writeAll(List.of(), "제목", "본문", Map.of());

        ArgumentCaptor<List<NotificationOutbox>> captor = ArgumentCaptor.forClass(List.class);
        then(notificationOutboxRepository).should().saveAllInBatch(captor.capture());
        assertThat(captor.getValue())
                .extracting(NotificationOutbox::getRecipientAddress)
                .containsExactly("t1", "t2");
        then(notificationOutboxRepository).should(never()).save(any());
    }
}
