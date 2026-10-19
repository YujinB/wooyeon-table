package com.wooyeon.table.domain.matching.domain.repository;

import com.wooyeon.table.domain.matching.domain.model.DiningTable;
import java.util.List;

public interface DiningTableRepository {

    List<DiningTable> saveAll(List<DiningTable> tables);

    List<DiningTable> findAllBySlotId(Long slotId);
}
