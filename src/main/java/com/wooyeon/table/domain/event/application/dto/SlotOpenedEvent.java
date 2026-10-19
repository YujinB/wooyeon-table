package com.wooyeon.table.domain.event.application.dto;

/** 운영자가 새 회차를 열었다 (다음 모집 시작 알림, PRD 14절) */
public record SlotOpenedEvent(Long slotId) {
}
