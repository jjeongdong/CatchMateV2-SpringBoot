package com.back.catchmate.chat.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.auth.infrastructure.JwtTokenProvider;
import com.back.catchmate.board.domain.Board;
import com.back.catchmate.board.infrastructure.BoardJpaRepository;
import com.back.catchmate.chat.domain.ChatRoom;
import com.back.catchmate.chat.domain.ChatRoomMember;
import com.back.catchmate.chat.domain.MessageType;
import com.back.catchmate.chat.domain.event.ChatMessageBroadcastEvent;
import com.back.catchmate.chat.infrastructure.ChatRoomJpaRepository;
import com.back.catchmate.chat.infrastructure.ChatRoomMemberJpaRepository;
import com.back.catchmate.chat.infrastructure.RedisChatMessageBroadcaster;
import com.back.catchmate.user.domain.Authority;
import com.back.catchmate.user.domain.User;
import com.back.catchmate.user.infrastructure.UserJpaRepository;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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

// 실제 임베디드 서버 + 실제 WebSocket/STOMP 핸드셰이크(JWT 인증, 구독 권한 검사 포함) + 실제 Redis 를 모두 거쳐
// "메시지가 정상적으로 오고 가는지" 를 검증한다.
// ChatRedisPubSubIntegrationTest 가 Redis <-> Subscriber 구간만 봤다면, 이 테스트는
// 발행(RedisChatMessageBroadcaster) -> Redis -> Subscriber -> 실제 인증된 STOMP 세션 수신까지 전 구간을 검증한다.
@SpringBootTest(
        classes = CatchmateApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=local")
class ChatMessageE2eStompTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private RedisChatMessageBroadcaster chatMessageBroadcaster;

    @Autowired
    private UserJpaRepository userRepository;

    @Autowired
    private ChatRoomJpaRepository chatRoomRepository;

    @Autowired
    private ChatRoomMemberJpaRepository chatRoomMemberRepository;

    @Autowired
    private BoardJpaRepository boardRepository;

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
        savedUser = userRepository.save(User.create(
                "KAKAO",
                "e2e-" + unique,
                "e2e-" + unique + "@test.com",
                "e2e-" + unique,
                'M',
                LocalDate.of(2000, 1, 1),
                1L,
                "https://example.com/profile.png",
                null));

        // chat_rooms.board_id 에 실제 FK(boards.board_id)가 걸려 있어 더미 게시글이 필요하다
        savedBoard = boardRepository.save(Board.create(
                savedUser.getId(),
                "e2e-" + unique,
                "e2e test board",
                4,
                null,
                null,
                false,
                null,
                null,
                false,
                LocalDateTime.now()));

        savedRoom = chatRoomRepository.save(ChatRoom.create(savedBoard.getId()));

        savedMember = chatRoomMemberRepository.save(
                ChatRoomMember.create(savedRoom, savedUser.getId(), 0L, LocalDateTime.now()));

        String token = tokenProvider.createAccessToken(savedUser.getId(), Authority.ROLE_USER.name());

        // when: 실제 WebSocket/STOMP 클라이언트로 CONNECT(JWT 인증) -> SUBSCRIBE(참여자 권한 검사)
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", token);

        StompSession session = stompClient
                .connectAsync(
                        "ws://localhost:" + port + "/ws/chat",
                        new WebSocketHttpHeaders(),
                        connectHeaders,
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

        ChatMessageBroadcastEvent event = new ChatMessageBroadcastEvent(
                999L,
                savedRoom.getId(),
                savedUser.getId(),
                savedUser.getNickName(),
                null,
                "E2E 테스트 메시지 " + unique,
                MessageType.TEXT,
                LocalDateTime.of(2026, 7, 28, 15, 0, 0));

        // 커밋 후 ChatMessageBroadcastListener 가 부르는 것과 같은 진입점
        chatMessageBroadcaster.broadcast(event);

        // then: 클라이언트가 실제로 프레임을 받았는지, 내용이 온전한지 확인
        byte[] received = receivedFrames.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();

        String body = new String(received, StandardCharsets.UTF_8);
        assertThat(body).contains("\"roomId\":" + savedRoom.getId());
        assertThat(body).contains("E2E 테스트 메시지 " + unique);

        session.disconnect();
    }
}
