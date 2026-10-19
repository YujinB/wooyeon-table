package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiningTableJpaRepository extends JpaRepository<DiningTable, Long> {

    @EntityGraph(attributePaths = "seats")
    List<DiningTable> findAllBySlotIdOrderByTableNo(Long slotId);

    @EntityGraph(attributePaths = "seats")
    Optional<DiningTable> findWithSeatsById(Long id);

    @Query("select s.table.id, s.userId from TableSeat s where s.userId in :userIds")
    List<Object[]> findTableIdAndUserIdByUserIdIn(Collection<Long> userIds);

    @Query("select t.slotId, count(t) from DiningTable t group by t.slotId")
    List<Object[]> countGroupBySlotId();

    @Query("select s.table.id from TableSeat s where s.applicationId = :applicationId")
    Optional<Long> findTableIdByApplicationId(Long applicationId);
}
