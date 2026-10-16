package com.wooyeon.table.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 회차 마감 자동 매칭 스케줄러. 기본은 꺼짐 — 가상데이터를 적재한 서버를 띄우자마자 지난 회차가 자동 매칭되지 않게 하고,
 * 데모에서는 "지금 마감 실행"으로 보여준다. 배포 환경에서 MATCHING_SCHEDULER_ENABLED=true 로 켠다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "matching.scheduler.enabled", havingValue = "true")
public class SchedulingConfig {
}
