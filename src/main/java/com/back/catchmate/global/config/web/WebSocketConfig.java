package com.back.catchmate.global.config.web;

import com.back.catchmate.global.config.security.StompAuthChannelInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;
    private final MeterRegistry meterRegistry;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;
    @Value("${websocket.inbound.pool-size:16}")
    private int inboundPoolSize;
    @Value("${websocket.inbound.queue-capacity:500}")
    private int inboundQueueCapacity;
    @Value("${websocket.outbound.core-pool-size:8}")
    private int outboundCorePoolSize;
    @Value("${websocket.outbound.max-pool-size:32}")
    private int outboundMaxPoolSize;
    @Value("${websocket.outbound.queue-capacity:1000}")
    private int outboundQueueCapacity;
    @Value("${websocket.heartbeat.interval-ms:25000}")
    private long heartbeatIntervalMs;

    /**
     * STOMP 엔드포인트 등록
     * 어느 경로로 WebSocket 연결을 수립할지 지정한다.
     * 예를 들어 클라이언트가 /ws/chat 경로로 연결을 시도하면 이 엔드포인트가 매핑된다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins);
    }

    // STOMP 브로커 설정
    // /sub, /queue 로 시작하는 목적지에 대해 SimpleBroker를 활성화한다.
    // /pub 로 시작하는 목적지는 @MessageMapping 메서드로 라우팅된다.
    // /user 로 시작하는 목적지는 특정 사용자에게 메시지를 보내는 데 사용된다.
    // 하트비트는 두 가지를 동시에 해결한다(둘 다 끄면 안 되는 이유).
    // ① 죽은 커넥션 감지 — 클라가 heartbeatIntervalMs 넘게 조용하면 브로커가 세션을 정리해
    //    SessionDisconnectEvent 가 발생한다. 이게 없으면 half-open 커넥션(비행기모드·앱 강제종료)에서
    //    user:online:{id}·user:focus:{id}(둘 다 TTL 없음)가 영구히 남아 그 유저의 채팅 푸시가 계속 억제된다.
    // ② idle 커넥션 유지 — ALB idle timeout(기본 60초) 안에 최소 2회는 프레임이 오가야 끊기지 않는다.
    // raw WebSocket 엔드포인트는 SockJS 하트비트(25초)를 못 받으므로 STOMP 레벨에서 걸어야 한다.
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue")
                .setTaskScheduler(wsHeartbeatScheduler())
                .setHeartbeatValue(new long[]{heartbeatIntervalMs, heartbeatIntervalMs});
        registry.setApplicationDestinationPrefixes("/pub");
        registry.setUserDestinationPrefix("/user");
    }

    // 전 세션을 순회하며 만료 검사 + 하트비트 프레임 발행만 하는 가벼운 작업이라 단일 스레드로 충분하다.
    // 실제 전송은 clientOutboundChannel(wsClientOutboundExecutor)이 담당한다.
    @Bean
    public TaskScheduler wsHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("WsHeartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
        registration.taskExecutor(wsClientInboundExecutor());
    }

    // 인바운드 큐를 유계화한다. 큐가 꽉 차면 CallerRunsPolicy 로 수신 스레드가 직접 처리 → 해당 커넥션에
    // TCP 백프레셔가 걸린다(어떤 프레임도 유실 없음 — CONNECT/SUBSCRIBE/SEND 전부 보호). 인바운드는 CPU-bound 라
    // 스레드 수는 늘리지 않고(core=max), 무제한 큐로 인한 지연 폭발/메모리 무한 증식을 큐 상한으로 막는 게 목적이다.
    @Bean
    public ThreadPoolTaskExecutor wsClientInboundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(inboundPoolSize);
        executor.setMaxPoolSize(inboundPoolSize);
        executor.setQueueCapacity(inboundQueueCapacity);
        executor.setThreadNamePrefix("WsInbound-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        meterRegistry.gauge("websocket.inbound.queue.size", executor,
                e -> e.getThreadPoolExecutor().getQueue().size());
        return executor;
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor(wsClientOutboundExecutor());
    }

    @Bean
    public ThreadPoolTaskExecutor wsClientOutboundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(outboundCorePoolSize);
        executor.setMaxPoolSize(outboundMaxPoolSize);
        executor.setQueueCapacity(outboundQueueCapacity);
        executor.setThreadNamePrefix("WsOutbound-");
        executor.setRejectedExecutionHandler((runnable, exec) ->
                meterRegistry.counter("websocket.outbound.dropped").increment());
        executor.initialize();
        return executor;
    }
}
