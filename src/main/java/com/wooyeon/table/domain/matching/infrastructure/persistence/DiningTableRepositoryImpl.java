package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import java.util.List;
import java.util.Optional;
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

    @Override
    public Optional<DiningTable> findById(Long id) {
        return jpaRepository.findWithSeatsById(id);
    }

    @Override
    public Optional<DiningTable> findBySeatApplicationId(Long applicationId) {
        return jpaRepository.findTableIdByApplicationId(applicationId).flatMap(jpaRepository::findWithSeatsById);
    }
}
