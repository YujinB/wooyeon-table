package com.wooyeon.table.domain.notification.infrastructure.push;

import com.wooyeon.table.domain.notification.domain.service.PushGateway;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 모의 푸시: 로그만 남기고 성공. 데모·테스트에서 발송 실패·재시도를 보여주려고
 * 특정 사용자에게 보내는 푸시를 실패시킬 수 있다 (운영자 API로 켜고 끔)
 */
@Slf4j
@Component
public class MockPushGateway implements PushGateway {

    private final Set<Long> failingUserIds = ConcurrentHashMap.newKeySet();

    @Override
    public void send(Long userId, String title, String body) {
        if (failingUserIds.contains(userId)) {
            throw new IllegalStateException("푸시 서버 응답 없음 (모의 장애)");
        }
        log.info("[모의 푸시] user={} {}", userId, title);
    }

    public void failFor(Long userId, boolean failing) {
        if (failing) {
            failingUserIds.add(userId);
        } else {
            failingUserIds.remove(userId);
        }
    }

    public Set<Long> failingUserIds() {
        return Set.copyOf(failingUserIds);
    }
}
