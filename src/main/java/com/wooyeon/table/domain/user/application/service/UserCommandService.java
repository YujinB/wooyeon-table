package com.wooyeon.table.domain.user.application.service;

import com.wooyeon.table.domain.user.domain.model.ConsentPolicy;
import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import com.wooyeon.table.global.exception.BusinessException;
import com.wooyeon.table.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;
    private final Clock clock;

    public void updateBasicInfo(Long userId, String name, String phone) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, String.valueOf(userId)))
                .updateBasicInfo(name, phone);
    }

    /** 개인정보 수집·이용과 운영 정책 동의 (ACC-05) */
    public void agree(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, String.valueOf(userId)))
                .agree(ConsentPolicy.VERSION, LocalDateTime.now(clock));
    }

    /** 같은 로그인 ID가 있으면 기존 사용자 id를 돌려준다 (적재 재실행 대비) */
    public Long createParticipantIfAbsent(String loginId, String name) {
        return userRepository.findByLoginId(loginId)
                .orElseGet(() -> userRepository.save(User.participant(loginId, name)))
                .getId();
    }
}
