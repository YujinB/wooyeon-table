package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DiningTableRepository {

    List<DiningTable> saveAll(List<DiningTable> tables);

    List<DiningTable> findAllBySlotId(Long slotId);

    Optional<DiningTable> findById(Long id);

    /** 이 사용자들이 앉았던 테이블별 사용자 (테이블 id → 그중 이 집합에 속한 사용자). 이전 만남 계산용 */
    Map<Long, List<Long>> findSeatedUsersByTable(Collection<Long> userIds);

    /** 회차별 확정 테이블 수 */
    Map<Long, Long> countBySlot();

    /** 이 신청이 앉은 테이블 */
    Optional<DiningTable> findBySeatApplicationId(Long applicationId);
}
