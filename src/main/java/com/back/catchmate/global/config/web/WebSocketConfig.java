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
        registration.taskExecutor()
                .corePoolSize(inboundPoolSize)
                .maxPoolSize(inboundPoolSize);
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
