package com.wooyeon.table.global.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envers가 남긴 엔티티 버전들을 시간순으로 비교해, 지정한 상태 필드가 바뀐 지점만 뽑는다
 */
@Component
public class AuditHistoryReader {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param fields 표시 이름 → 엔티티에서 값을 꺼내 보여 줄 문자열로 바꾸는 함수 (순서 유지되는 Map)
     */
    @Transactional(readOnly = true)
    public <T> List<StatusChange> changes(Class<T> type, String property, Object value,
                                          Map<String, Function<T, String>> fields) {
        @SuppressWarnings("unchecked")
        List<Object[]> revisions = AuditReaderFactory.get(entityManager).createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.property(property).eq(value))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        List<StatusChange> changes = new ArrayList<>();
        Map<Object, T> previousById = new HashMap<>();
        for (Object[] row : revisions) {
            @SuppressWarnings("unchecked")
            T entity = (T) row[0];
            AuditRevision revision = (AuditRevision) row[1];
            if (entity == null) {
                continue;
            }
            LocalDateTime at = LocalDateTime.ofInstant(Instant.ofEpochMilli(revision.getTimestamp()), SEOUL);
            Object id = entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(entity);
            T previous = previousById.get(id);
            for (Map.Entry<String, Function<T, String>> field : fields.entrySet()) {
                String now = field.getValue().apply(entity);
                String before = previous == null ? null : field.getValue().apply(previous);
                if (previous == null || !Objects.equals(before, now)) {
                    changes.add(new StatusChange(at, revision.getActor(), field.getKey(), before, now));
                }
            }
            previousById.put(id, entity);
        }
        return changes;
    }
}
