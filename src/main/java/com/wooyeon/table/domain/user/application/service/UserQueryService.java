package com.wooyeon.table.domain.user.application.service;

import com.wooyeon.table.domain.user.domain.model.User;
import com.wooyeon.table.domain.user.domain.repository.UserRepository;
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

    /** 사용자 id → 이름 (운영자 화면 표시용) */
    public Map<Long, String> findNames(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllByIdIn(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));
    }
}
