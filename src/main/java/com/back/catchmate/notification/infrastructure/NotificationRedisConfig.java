package com.back.catchmate.notification.infrastructure;

import com.back.catchmate.global.config.data.RedisTopicSubscription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class NotificationRedisConfig {

    @Bean
    public MessageListenerAdapter notificationListenerAdapter(NotificationRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onNotification");
    }

    // 토픽 이름은 서버 간 계약이라 바꾸지 않는다 (롤링 배포 중 옛·새 서버가 같은 토픽을 쓴다).
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic("catchmate-notification-topic");
    }

    @Bean
    public RedisTopicSubscription notificationSubscription(
            MessageListenerAdapter notificationListenerAdapter, ChannelTopic notificationTopic) {
        return new RedisTopicSubscription(notificationListenerAdapter, notificationTopic);
    }
}
