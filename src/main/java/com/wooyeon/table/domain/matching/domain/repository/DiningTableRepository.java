package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import java.util.List;
import java.util.Optional;

public interface DiningTableRepository {

    List<DiningTable> saveAll(List<DiningTable> tables);

    List<DiningTable> findAllBySlotId(Long slotId);

    Optional<DiningTable> findById(Long id);

    /** 이 신청이 앉은 테이블 */
    Optional<DiningTable> findBySeatApplicationId(Long applicationId);
}
