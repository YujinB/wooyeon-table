package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackJpaRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findByApplicationId(Long applicationId);

    List<Feedback> findAllByTableIdOrderByIdAsc(Long tableId);
}
