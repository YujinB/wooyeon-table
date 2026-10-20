package com.wooyeon.table.domain.feedback.domain.repository;

import com.wooyeon.table.domain.feedback.domain.model.Feedback;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository {

    /** 바로 flush해 중복 제출(유일 제약)을 이 요청에서 드러낸다 */
    Feedback save(Feedback feedback);

    Optional<Feedback> findByApplicationId(Long applicationId);

    List<Feedback> findAllByTableId(Long tableId);

    List<Feedback> findAllBySlotId(Long slotId);
}
