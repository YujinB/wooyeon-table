package com.wooyeon.table.domain.chat.domain.model;

/** 시스템 메시지 종류(방마다 하나씩) + 참가자 대화 */
public enum ChatMessageKind {
    /** 단체방 생성·환영 */
    WELCOME,
    /** 시간·장소 (INF-05) */
    PLACE,
    /** 테이블 조합 큐레이션 (INF-05) */
    CURATION,
    /** 취소·노쇼 정책 (INF-05) */
    POLICY,
    /** 행사 1일 전 리마인드 (INF-07) */
    REMINDER,
    /** 대화 주제·아이스브레이킹 (CON-03, W3-3) */
    CONTENT,
    CHAT
}
