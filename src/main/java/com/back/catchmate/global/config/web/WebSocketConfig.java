package com.back.catchmate.global.config.web;

import com.back.catchmate.global.config.security.StompAuthChannelInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue");
        registry.setApplicationDestinationPrefixes("/pub");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();

        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins);
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
