package com.wooyeon.table.domain.chat.application.dto;

/** 단체방 멤버 (내부용: userId 포함, 참가자 응답에는 그대로 내보내지 않는다) */
public record TableMember(int seatNo, String alias, String intro, Long userId, Long applicationId) {
}
