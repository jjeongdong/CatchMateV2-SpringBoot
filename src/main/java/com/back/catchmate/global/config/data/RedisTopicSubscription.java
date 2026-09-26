package com.back.catchmate.global.config.data;

import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.Topic;

// BC 가 자기 Pub/Sub 구독을 등록한다. RedisConfig 의 리스너 컨테이너가 모두 모아 붙인다.
public record RedisTopicSubscription(MessageListener listener, Topic topic) {}
