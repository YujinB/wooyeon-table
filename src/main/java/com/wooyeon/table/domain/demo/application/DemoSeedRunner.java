package com.wooyeon.table.domain.demo.application;

import com.wooyeon.table.domain.user.application.DemoAccountInitializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * DEMO_SEED_ENABLED=true일 때 서버 시작 시 시연용 데이터를 한 번 넣는다 (데모 계정·식당 등록 뒤).
 * 이미 시드가 있으면 아무것도 하지 않으므로, 처음부터 다시 하려면 DB를 비우고 띄운다
 */
@Slf4j
@Component
@Order(10)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
public class DemoSeedRunner implements ApplicationRunner {

    private final DemoSeedService demoSeedService;

    @Override
    public void run(ApplicationArguments args) {
        if (!demoSeedService.seedIfEmpty(DemoAccountInitializer.PARTICIPANT_LOGIN_IDS)) {
            log.info("데모 시드가 이미 있어 건너뜀");
        }
    }
}
