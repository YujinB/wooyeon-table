package com.wooyeon.table.domain.user.application.service;

import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

    private final UserRepository userRepository;

    /** 같은 로그인 ID가 있으면 기존 사용자 id를 돌려준다 (적재 재실행 대비) */
    public Long createParticipantIfAbsent(String loginId, String name) {
        return userRepository.findByLoginId(loginId)
                .orElseGet(() -> userRepository.save(User.participant(loginId, name)))
                .getId();
    }
}
