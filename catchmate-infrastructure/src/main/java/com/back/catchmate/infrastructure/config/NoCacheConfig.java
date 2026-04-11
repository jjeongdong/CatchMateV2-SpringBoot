package com.back.catchmate.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "cache.enabled", havingValue = "false")
public class NoCacheConfig {

    @Bean
    public CacheManager redisCacheManager() {
        return new NoOpCacheManager();
    }
}
