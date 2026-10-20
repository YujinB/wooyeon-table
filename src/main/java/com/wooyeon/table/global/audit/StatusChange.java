package com.wooyeon.table.global.audit;

import java.time.LocalDateTime;

/**
 * 상태 변경 한 건: 언제·누가·무엇을·이전 값 → 새 값 (PRD 16절, CTO 온보딩 Audit log)
 *
 * @param from 처음 만들어질 때는 null
 */
public record StatusChange(LocalDateTime at, String actor, String field, String from, String to) {
}
