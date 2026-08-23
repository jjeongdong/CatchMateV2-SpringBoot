package com.back.catchmate.chat.event;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.auth.infra.JwtTokenProvider;
import com.back.catchmate.board.entity.Board;
import com.back.catchmate.board.repository.BoardRepository;
import com.back.catchmate.chat.event.ChatMessageRedisPublisher;
import com.back.catchmate.chat.entity.ChatRoom;
import com.back.catchmate.chat.entity.ChatRoomMember;
import com.back.catchmate.chat.repository.ChatRoomMemberRepository;
import com.back.catchmate.chat.repository.ChatRoomRepository;
import com.back.catchmate.chat.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.entity.MessageType;
import com.back.catchmate.user.entity.User;
import com.back.catchmate.user.repository.UserRepository;
import com.back.catchmate.user.entity.Authority;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 임베디드 서버 + 실제 WebSocket/STOMP 핸드셰이크(JWT 인증, 구독 권한 검사 포함) + 실제 Redis 를 모두 거쳐
// "메시지가 정상적으로 오고 가는지" 를 검증한다.
// ChatRedisPubSubIntegrationTest 가 Redis <-> Subscriber 구간만 봤다면, 이 테스트는
// 발행(ChatMessageRedisPublisher) -> Redis -> Subscriber -> 실제 인증된 STOMP 세션 수신까지 전 구간을 검증한다.
@SpringBootTest(
        classes = CatchmateApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=local"
)
class ChatMessageE2eStompTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private ChatMessageRedisPublisher chatMessageRedisPublisher;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Autowired
    private BoardRepository boardRepository;

    private User savedUser;
    private Board savedBoard;
    private ChatRoom savedRoom;
    private ChatRoomMember savedMember;

    @AfterEach
    void cleanUp() {
        if (savedMember != null) chatRoomMemberRepository.deleteById(savedMember.getId());
        if (savedRoom != null) chatRoomRepository.deleteById(savedRoom.getId());
        if (savedBoard != null) boardRepository.deleteById(savedBoard.getId());
        if (savedUser != null) userRepository.deleteById(savedUser.getId());
    }

    @Test
    @DisplayName("실제 WebSocket/STOMP 연결로 채팅 메시지가 정상적으로 오고 간다")
    void 실제_stomp_연결로_메시지가_정상적으로_오고_간다() throws Exception {
        // given: 테스트용 유저 + 채팅방 + 참여자 (실제 DB)
        long unique = System.nanoTime();
        savedUser = userRepository.save(User.builder()
                .clubId(1L)
                .email("e2e-" + unique + "@test.com")
                .provider("KAKAO")
                .providerId("e2e-" + unique)
                .gender('M')
                .nickName("e2e-" + unique)
                .birthDate(LocalDate.of(2000, 1, 1))
                .profileImageUrl("https://example.com/profile.png")
                .allAlarm('Y')
                .chatAlarm('Y')
                .enrollAlarm('Y')
                .eventAlarm('Y')
                .authority(Authority.ROLE_USER)
                .reported(false)
                .build());

        // chat_rooms.board_id 에 실제 FK(boards.board_id)가 걸려 있어 더미 게시글이 필요하다
        savedBoard = boardRepository.save(Board.builder()
                .title("e2e-" + unique)
                .content("e2e test board")
                .maxPerson(4)
                .currentPerson(1)
                .userId(savedUser.getId())
                .completed(false)
                .liftUpDate(LocalDateTime.now())
                .build());

        savedRoom = chatRoomRepository.save(ChatRoom.builder()
                .boardId(savedBoard.getId())
                .lastMessageSequence(0L)
                .build());

        savedMember = chatRoomMemberRepository.save(ChatRoomMember.builder()
                .chatRoom(savedRoom)
                .userId(savedUser.getId())
                .joinedAt(LocalDateTime.now())
                .lastReadSequence(0L)
                .build());

        String token = tokenProvider.createAccessToken(savedUser.getId(), Authority.ROLE_USER.name());

        // when: 실제 WebSocket/STOMP 클라이언트로 CONNECT(JWT 인증) -> SUBSCRIBE(참여자 권한 검사)
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", token);

        StompSession session = stompClient
                .connectAsync("ws://localhost:" + port + "/ws/chat", new WebSocketHttpHeaders(), connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<byte[]> receivedFrames = new LinkedBlockingQueue<>();
        session.subscribe("/sub/chat/room/" + savedRoom.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                receivedFrames.add((byte[]) payload);
            }
        });
        Thread.sleep(500); // 서버가 SUBSCRIBE 를 처리하고 브로커에 등록할 시간

        ChatMessageBroadcastEvent event = ChatMessageBroadcastEvent.builder()
                .messageId(999L)
                .roomId(savedRoom.getId())
                .senderId(savedUser.getId())
                .senderNickname(savedUser.getNickName())
                .content("E2E 테스트 메시지 " + unique)
                .messageType(MessageType.TEXT)
                .createdAt(LocalDateTime.of(2026, 7, 28, 15, 0, 0))
                .build();

        // ChatMessageRedisPublisher 가 커밋 후 실제로 호출되는 것과 동일한 진입점
        chatMessageRedisPublisher.publishChat(event);

        // then: 클라이언트가 실제로 프레임을 받았는지, 내용이 온전한지 확인
        byte[] received = receivedFrames.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        String body = new String(received, StandardCharsets.UTF_8);
        assertThat(body).contains("\"roomId\":" + savedRoom.getId());
        assertThat(body).contains("E2E 테스트 메시지 " + unique);

        session.disconnect();
    }
}
