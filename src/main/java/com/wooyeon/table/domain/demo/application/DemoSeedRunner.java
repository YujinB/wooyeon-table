package com.wooyeon.table.domain.demo.application;

import com.wooyeon.table.domain.user.application.DemoAccountInitializer;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
@ConditionalOnProperty(name = "demo.seed.enabled", havingValue = "true")
@RequiredArgsConstructor
public class DemoSeedRunner implements ApplicationRunner {

    private final DemoSeedService demoSeedService;

    /** 신청자 엑셀 경로 (DEMO_APPLICANTS_FILE). 비우거나 파일이 없으면 가상데이터 없이 시드 */
    @Value("${demo.seed.applicants-file:}")
    private String applicantsFile;

    @Override
    public void run(ApplicationArguments args) {
        Path file = applicantsFile.isBlank() ? null : Path.of(applicantsFile);
        if (file != null && !Files.isRegularFile(file)) {
            log.warn("신청자 엑셀이 없어 가상데이터 없이 시드: {}", file);
            file = null;
        }
        if (!demoSeedService.seedIfEmpty(DemoAccountInitializer.PARTICIPANT_LOGIN_IDS, file)) {
            log.info("데모 시드가 이미 있어 건너뜀");
        }
    }
}
