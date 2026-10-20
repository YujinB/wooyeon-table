package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import com.wooyeon.table.domain.feedback.domain.repository.FeedbackRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FeedbackRepositoryImpl implements FeedbackRepository {

    private final FeedbackJpaRepository jpaRepository;

    @Override
    public Feedback save(Feedback feedback) {
        return jpaRepository.saveAndFlush(feedback);
    }

    @Override
    public Optional<Feedback> findByApplicationId(Long applicationId) {
        return jpaRepository.findByApplicationId(applicationId);
    }

    @Override
    public List<Feedback> findAllByTableId(Long tableId) {
        return jpaRepository.findAllByTableIdOrderByIdAsc(tableId);
    }

    @Override
    public List<Feedback> findAllBySlotId(Long slotId) {
        return jpaRepository.findAllBySlotIdOrderByIdAsc(slotId);
    }
}
