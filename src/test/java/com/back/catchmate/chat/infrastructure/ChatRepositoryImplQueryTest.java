package com.back.catchmate.chat.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.chat.domain.ChatMessage;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.ReadSequence;
import com.back.catchmate.global.config.data.JpaAuditingConfig;
import com.back.catchmate.global.config.data.QuerydslConfig;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

// 운영 RDS 를 가리키는 dev 프로필을 끄고 컨테이너 DB 에만 붙는다.
// chat_rooms.board_id·chat_room_members 등의 FK 는 ddl-auto 가 만든 스키마 기준이라 더미 행만으로 충분하다.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@ImportAutoConfiguration(JdbcTemplateAutoConfiguration.class)
@Import({
    QuerydslConfig.class,
    JpaAuditingConfig.class,
    ChatRoomRepositoryImpl.class,
    ChatRoomMemberRepositoryImpl.class,
    ChatMessageRepositoryImpl.class
})
@Testcontainers(disabledWithoutDocker = true)
class ChatRepositoryImplQueryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 1, 12, 0);

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @Autowired
    private ChatRoomRepositoryImpl chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepositoryImpl chatRoomMemberRepository;

    @Autowired
    private ChatMessageRepositoryImpl chatMessageRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("메시지 기록은 커서 이전 것을 limit 개만 오래된 순으로, 동기화는 커서 이후를 오래된 순으로 준다")
    void historyAndSync() {
        // given
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(10L));
        ChatMessage m1 = chatMessageRepository.save(ChatMessage.text(room, 1L, "1", 1L));
        ChatMessage m2 = chatMessageRepository.save(ChatMessage.text(room, 1L, "2", 2L));
        ChatMessage m3 = chatMessageRepository.save(ChatMessage.text(room, 1L, "3", 3L));

        // when & then
        assertThat(chatMessageRepository.findHistory(room.getId(), null, 2))
                .extracting(ChatMessage::getId)
                .containsExactly(m2.getId(), m3.getId());
        assertThat(chatMessageRepository.findHistory(room.getId(), m2.getId(), 2))
                .extracting(ChatMessage::getId)
                .containsExactly(m1.getId());
        assertThat(chatMessageRepository.findAfter(room.getId(), m1.getId(), 10))
                .extracting(ChatMessage::getId)
                .containsExactly(m2.getId(), m3.getId());
    }

    @Test
    @DisplayName("방별 마지막 메시지는 시스템 메시지를 건너뛴 TEXT 다")
    void lastText() {
        // given
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(10L));
        ChatRoom empty = chatRoomRepository.save(ChatRoom.create(11L));
        ChatMessage text = chatMessageRepository.save(ChatMessage.text(room, 1L, "안녕", 1L));
        chatMessageRepository.save(ChatMessage.joined(room, 2L, "영희", 1L));

        // when
        Map<Long, ChatMessage> lastByRoom =
                chatMessageRepository.findLastTextByChatRoomIds(List.of(room.getId(), empty.getId()));

        // then
        assertThat(lastByRoom).containsOnlyKeys(room.getId());
        assertThat(lastByRoom.get(room.getId()).getId()).isEqualTo(text.getId());
        assertThat(chatMessageRepository.findLastText(room.getId()))
                .map(ChatMessage::getId)
                .contains(text.getId());
    }

    @Test
    @DisplayName("내 채팅방 목록은 활성 멤버인 방만 최신 생성순으로 세고 페이징한다")
    void roomsByMember() {
        // given
        ChatRoom older = chatRoomRepository.save(ChatRoom.create(10L));
        ChatRoom newer = chatRoomRepository.save(ChatRoom.create(11L));
        ChatRoom leftRoom = chatRoomRepository.save(ChatRoom.create(12L));
        chatRoomMemberRepository.save(ChatRoomMember.create(older, 1L, 0L, NOW));
        chatRoomMemberRepository.save(ChatRoomMember.create(newer, 1L, 0L, NOW));
        ChatRoomMember left = ChatRoomMember.create(leftRoom, 1L, 0L, NOW);
        left.leave(NOW);
        chatRoomMemberRepository.save(left);

        // when & then (같은 시각이면 id 내림차순)
        assertThat(chatRoomRepository.findAllByMemberUserId(1L, 0, 10))
                .extracting(ChatRoom::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(chatRoomRepository.findAllByMemberUserId(1L, 1, 1))
                .extracting(ChatRoom::getId)
                .containsExactly(older.getId());
        assertThat(chatRoomRepository.countByMemberUserId(1L)).isEqualTo(2);
        assertThat(chatRoomMemberRepository.findActiveByChatRoomIdsAndUserId(
                        List.of(older.getId(), leftRoom.getId()), 1L))
                .hasSize(1);
    }

    @Test
    @DisplayName("버퍼 반영은 기존 값보다 클 때만 갱신하고, 읽음 반영은 퇴장한 멤버를 건너뛴다")
    void batchUpdatesAreMonotonic() {
        // given
        ChatRoom room = chatRoomRepository.save(ChatRoom.create(10L));
        ChatRoomMember active = chatRoomMemberRepository.save(ChatRoomMember.create(room, 1L, 5L, NOW));
        ChatRoomMember left = ChatRoomMember.create(room, 2L, 0L, NOW);
        left.leave(NOW);
        chatRoomMemberRepository.save(left);
        entityManager.flush();

        // when
        chatRoomRepository.updateMaxSequences(Map.of(room.getId(), 9L));
        chatRoomRepository.updateMaxSequences(Map.of(room.getId(), 4L));
        chatRoomMemberRepository.updateLastReadSequences(
                List.of(new ReadSequence(room.getId(), 1L, 3L), new ReadSequence(room.getId(), 2L, 7L)));
        entityManager.clear();

        // then
        assertThat(chatRoomRepository.getById(room.getId()).getLastMessageSequence())
                .isEqualTo(9L);
        assertThat(entityManager.find(ChatRoomMember.class, active.getId()).getLastReadSequence())
                .isEqualTo(5L);
        assertThat(entityManager.find(ChatRoomMember.class, left.getId()).getLastReadSequence())
                .isZero();
    }
}
