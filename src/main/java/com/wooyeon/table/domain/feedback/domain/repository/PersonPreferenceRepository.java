package com.wooyeon.table.domain.feedback.domain.repository;

import com.wooyeon.table.domain.feedback.domain.model.PersonPreference;
import com.wooyeon.table.domain.feedback.domain.model.PreferenceType;
import java.util.Collection;
import java.util.List;

public interface PersonPreferenceRepository {

    List<PersonPreference> saveAll(List<PersonPreference> preferences);

    /** 내가 이 테이블에서 남긴 선호 */
    List<PersonPreference> findByFromUserIdAndTableId(Long fromUserId, Long tableId);

    /** 두 사람 모두 이 사용자 집합에 속한 선호 (다음 매칭 후보끼리의 관계) */
    List<PersonPreference> findAmong(Collection<Long> userIds, PreferenceType type);
}
