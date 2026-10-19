package com.wooyeon.table.domain.resolution.domain.repository;

import com.wooyeon.table.domain.resolution.domain.model.Resolution;
import java.util.Optional;

public interface ResolutionRepository {

    /** 바로 flush해 유일 제약 위반을 이 요청 안에서 드러낸다 */
    Resolution save(Resolution resolution);

    Optional<Resolution> findByApplicationId(Long applicationId);
}
