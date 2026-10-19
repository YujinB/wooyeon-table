package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.PersonPreference;
import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonPreferenceJpaRepository extends JpaRepository<PersonPreference, Long> {

    List<PersonPreference> findAllByFromUserIdAndTableIdOrderByIdAsc(Long fromUserId, Long tableId);

    List<PersonPreference> findAllByTypeAndFromUserIdInAndToUserIdIn(PreferenceType type, Collection<Long> fromUserIds,
                                                                    Collection<Long> toUserIds);
}
