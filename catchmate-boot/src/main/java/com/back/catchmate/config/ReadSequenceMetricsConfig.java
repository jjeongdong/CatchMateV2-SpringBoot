package com.back.catchmate.config;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ReadSequenceMetricsConfig {

    @Bean
    public MeterFilter hikariReadSequenceModeTagFilter(
            @Value("${chat.read-sequence.mode:V4_LUA_BUFFERED}") String mode) {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if (id.getName().startsWith("hikaricp.connections")) {
                    return id.withTag(Tag.of("mode", mode));
                }
                return id;
            }
        };
    }
}
