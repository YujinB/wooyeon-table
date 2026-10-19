package com.wooyeon.table.domain.user.application;

import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import com.wooyeon.table.global.auth.AuthProperties;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 심사용 테스트 계정(운영자 1, 참가자 3)을 서버 시작 시 만든다. 이미 있으면 비밀번호만 맞춘다 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class DemoAccountInitializer implements ApplicationRunner {

    public static final String ADMIN_LOGIN_ID = "admin";
    public static final List<String> PARTICIPANT_LOGIN_IDS = List.of("demo1", "demo2", "demo3", "demo4", "demo5");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode(authProperties.demoPassword());
        ensure(User.admin(ADMIN_LOGIN_ID, "운영자"), hash);
        for (int i = 0; i < PARTICIPANT_LOGIN_IDS.size(); i++) {
            ensure(User.participant(PARTICIPANT_LOGIN_IDS.get(i), "체험참가자" + (i + 1)), hash);
        }
        log.info("데모 계정 준비: {}, {}", ADMIN_LOGIN_ID, PARTICIPANT_LOGIN_IDS);
    }

    private void ensure(User candidate, String hash) {
        User user = userRepository.findByLoginId(candidate.getLoginId())
                .orElseGet(() -> userRepository.save(candidate));
        user.changePasswordHash(hash);
    }
}
