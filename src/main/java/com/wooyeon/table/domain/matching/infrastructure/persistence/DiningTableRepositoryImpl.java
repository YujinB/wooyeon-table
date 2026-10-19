package com.wooyeon.table.domain.matching.infrastructure.persistence;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import com.wooyeon.table.domain.matching.domain.repository.DiningTableRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
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
    public Map<Long, List<Long>> findSeatedUsersByTable(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return jpaRepository.findTableIdAndUserIdByUserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(row -> (Long) row[0],
                        Collectors.mapping(row -> (Long) row[1], Collectors.toList())));
    }

    @Override
    public Optional<DiningTable> findBySeatApplicationId(Long applicationId) {
        return jpaRepository.findTableIdByApplicationId(applicationId).flatMap(jpaRepository::findWithSeatsById);
    }
}
