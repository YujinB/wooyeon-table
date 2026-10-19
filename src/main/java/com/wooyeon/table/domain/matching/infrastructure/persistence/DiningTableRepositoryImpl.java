package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DiningTableRepositoryImpl implements DiningTableRepository {

    private final DiningTableJpaRepository jpaRepository;

    @Override
    public List<DiningTable> saveAll(List<DiningTable> tables) {
        return jpaRepository.saveAll(tables);
    }

    @Override
    public List<DiningTable> findAllBySlotId(Long slotId) {
        return jpaRepository.findAllBySlotIdOrderByTableNo(slotId);
    }
}
