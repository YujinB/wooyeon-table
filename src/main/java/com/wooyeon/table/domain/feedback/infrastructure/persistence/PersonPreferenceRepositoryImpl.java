package com.wooyeon.table.domain.feedback.infrastructure.persistence;

import com.wooyeon.table.domain.feedback.domain.model.PersonPreference;
import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import com.wooyeon.table.domain.feedback.domain.repository.PersonPreferenceRepository;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PersonPreferenceRepositoryImpl implements PersonPreferenceRepository {

    private final PersonPreferenceJpaRepository jpaRepository;

    @Override
    public List<PersonPreference> saveAll(List<PersonPreference> preferences) {
        List<PersonPreference> saved = jpaRepository.saveAll(preferences);
        jpaRepository.flush();
        return saved;
    }

    @Override
    public List<PersonPreference> findByFromUserIdAndTableId(Long fromUserId, Long tableId) {
        return jpaRepository.findAllByFromUserIdAndTableIdOrderByIdAsc(fromUserId, tableId);
    }

    @Override
    public List<PersonPreference> findAmong(Collection<Long> userIds, PreferenceType type) {
        return userIds.isEmpty() ? List.of()
                : jpaRepository.findAllByTypeAndFromUserIdInAndToUserIdIn(type, userIds, userIds);
    }
}
