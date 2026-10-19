package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiningTableJpaRepository extends JpaRepository<DiningTable, Long> {

    @EntityGraph(attributePaths = "seats")
    List<DiningTable> findAllBySlotIdOrderByTableNo(Long slotId);
}
