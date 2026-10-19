package com.wooyeon.table.domain.application.domain.repository;

import com.wooyeon.table.domain.application.domain.model.Application;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {

    Application save(Application application);

    Optional<Application> findById(Long id);

    List<Application> findAllByIdIn(Collection<Long> ids);

    /** 그 회차를 고른, 신청 완료·결제 완료·매칭 대기 신청 (MAT-01 후보) */
    List<Application> findMatchCandidates(Long slotId);

    boolean existsByUserId(Long userId);

    /** 사용자의 신청 (최근 것부터) */
    List<Application> findAllByUserId(Long userId);
}
