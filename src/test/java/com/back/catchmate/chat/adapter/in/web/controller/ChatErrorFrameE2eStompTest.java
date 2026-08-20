package com.back.catchmate.chat.adapter.in.web.controller;

import com.back.catchmate.CatchmateApplication;
import com.back.catchmate.auth.application.port.out.external.TokenProvider;
import com.back.catchmate.user.adapter.out.persistence.entity.UserEntity;
import com.back.catchmate.user.adapter.out.persistence.repository.JpaUserRepository;
import com.back.catchmate.user.domain.model.Authority;
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
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

// 커밋 전 전송 실패가 발신자에게 실제로 되돌아오는지를 전 구간(실제 STOMP 핸드셰이크 + JWT 인증 +
// /user/queue/errors 구독 인가 + @MessageExceptionHandler + @SendToUser)으로 검증한다.
// 이 경로가 깨지면 예외는 서버 로그에만 남고 발신자는 메시지가 사라진 걸 알 수 없다.
@SpringBootTest(
        classes = CatchmateApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.profiles.active=local"
)
class ChatErrorFrameE2eStompTest {
    private static final long NOT_JOINED_ROOM_ID = 999_999_999L;

    @LocalServerPort
    private int port;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private JpaUserRepository userRepository;

    private UserEntity savedUser;
    private StompSession session;

    @AfterEach
    void cleanUp() {
        if (session != null && session.isConnected()) session.disconnect();
        if (savedUser != null) userRepository.deleteById(savedUser.getId());
    }

    @Test
    @DisplayName("참여하지 않은 채팅방에 전송하면 재시도 불가 에러가 발신자에게만 되돌아온다")
    void 참여하지_않은_방_전송시_에러_프레임을_받는다() throws Exception {
        // given
        BlockingQueue<byte[]> errorFrames = connectAndSubscribeErrors();

        // when
        send("""
                {"chatRoomId":%d,"content":"안녕하세요","messageType":"TEXT"}
                """.formatted(NOT_JOINED_ROOM_ID));

        // then
        String body = pollErrorFrame(errorFrames);
        assertThat(body).contains("\"code\":\"CHATROOM_MEMBER_NOT_FOUND\"");
        assertThat(body).contains("\"chatRoomId\":" + NOT_JOINED_ROOM_ID);
        // 도메인 규칙 위반 → 재전송해도 동일 실패
        assertThat(body).contains("\"retryable\":false");
    }

    @Test
    @DisplayName("내용이 비어 있으면 검증 메시지를 담은 에러가 발신자에게만 되돌아온다")
    void 내용이_비어있으면_검증_에러_프레임을_받는다() throws Exception {
        // given
        BlockingQueue<byte[]> errorFrames = connectAndSubscribeErrors();

        // when
        send("""
                {"chatRoomId":%d,"content":"","messageType":"TEXT"}
                """.formatted(NOT_JOINED_ROOM_ID));

        // then: @Valid 가 컨트롤러 진입 시점에 걸러 멤버십 조회(DB)까지 가지 않는다
        String body = pollErrorFrame(errorFrames);
        assertThat(body).contains("\"code\":\"BAD_REQUEST\"");
        assertThat(body).contains("메시지 내용은 필수입니다.");
        assertThat(body).contains("\"retryable\":false");
    }

    private BlockingQueue<byte[]> connectAndSubscribeErrors() throws Exception {
        long unique = System.nanoTime();
        savedUser = userRepository.save(UserEntity.builder()
                .clubId(1L)
                .email("err-" + unique + "@test.com")
                .provider("KAKAO")
                .providerId("err-" + unique)
                .gender('M')
                .nickName("err-" + unique)
                .birthDate(LocalDate.of(2000, 1, 1))
                .profileImageUrl("https://example.com/profile.png")
                .allAlarm('Y')
                .chatAlarm('Y')
                .enrollAlarm('Y')
                .eventAlarm('Y')
                .authority(Authority.ROLE_USER)
                .reported(false)
                .build());

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization",
                tokenProvider.createAccessToken(savedUser.getId(), Authority.ROLE_USER.name()));

        session = new WebSocketStompClient(new StandardWebSocketClient())
                .connectAsync("ws://localhost:" + port + "/ws/chat", new WebSocketHttpHeaders(), connectHeaders,
                        new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        BlockingQueue<byte[]> errorFrames = new LinkedBlockingQueue<>();
        // 화이트리스트에 등록돼 있지 않으면 이 구독 자체가 거부된다
        session.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                errorFrames.add((byte[]) payload);
            }
        });
        Thread.sleep(500); // 서버가 SUBSCRIBE 를 처리하고 브로커에 등록할 시간
        return errorFrames;
    }

    private void send(String json) {
        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/pub/chat/message");
        sendHeaders.setContentType(MimeTypeUtils.APPLICATION_JSON);
        session.send(sendHeaders, json.getBytes(StandardCharsets.UTF_8));
    }

    private String pollErrorFrame(BlockingQueue<byte[]> errorFrames) throws Exception {
        byte[] received = errorFrames.poll(5, TimeUnit.SECONDS);
        assertThat(received).isNotNull();
        return new String(received, StandardCharsets.UTF_8);
    }
}
