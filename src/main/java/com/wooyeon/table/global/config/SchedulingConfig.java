package com.wooyeon.table.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 켜기. 각 스케줄러는 따로 켜고 끈다:
 * <ul>
 *   <li>회차 마감 자동 매칭: 기본 꺼짐. 가상데이터를 적재한 서버를 띄우자마자 지난 회차가 자동 매칭되지 않게 하고,
 *       데모에서는 "지금 마감 실행"으로 보여준다. 배포 환경에서 MATCHING_SCHEDULER_ENABLED=true</li>
 *   <li>알림 발송(리마인드·재시도): 기본 켜짐. NOTIFICATION_DISPATCHER_ENABLED=false로 끔</li>
 * </ul>
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
