package com.wooyeon.table.domain.matching.application.config;

import com.wooyeon.table.domain.matching.domain.model.MatchingPolicy;
import com.wooyeon.table.domain.matching.domain.service.MatchingEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MatchingEngineConfig {

    /** 운영자 가중치 설정(MAT-15)을 붙이기 전까지는 기본값 */
    @Bean
    public MatchingPolicy matchingPolicy() {
        return MatchingPolicy.defaults();
    }

    @Bean
    public MatchingEngine matchingEngine(MatchingPolicy matchingPolicy) {
        return new MatchingEngine(matchingPolicy);
    }
}
