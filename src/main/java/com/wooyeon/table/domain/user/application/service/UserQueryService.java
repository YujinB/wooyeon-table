package com.wooyeon.table.domain.user.application.service;

import com.wooyeon.table.domain.user.application.dto.MyConsent;
import com.wooyeon.table.domain.user.application.dto.UserInfo;
import com.wooyeon.table.domain.user.domain.model.ConsentPolicy;
import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    /** 지금 정책 버전에 동의했는지 (ACC-05) */
    public boolean hasConsented(Long userId) {
        return userRepository.findById(userId).map(user -> user.hasConsented(ConsentPolicy.VERSION)).orElse(false);
    }

    public MyConsent myConsent(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, String.valueOf(userId)));
        boolean agreed = user.hasConsented(ConsentPolicy.VERSION);
        return new MyConsent(agreed, ConsentPolicy.VERSION, agreed ? user.getConsentedAt() : null,
                ConsentPolicy.SECTIONS);
    }

    public boolean existsByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId).isPresent();
    }

    public UserInfo getUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserInfo::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, String.valueOf(userId)));
    }

    /** 사용자 id → 이름 (운영자 화면 표시용) */
    public Map<Long, String> findNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
